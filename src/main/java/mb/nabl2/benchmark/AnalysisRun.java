package mb.nabl2.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.IFragment.FragmentPiece;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Injector;

import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.extract.SpoofaxTestCaseExtractor;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;

public class AnalysisRun {
    public static void main(String[] args) throws Throwable {
        String sptPath = args[0];
        String languageUnderTestPath = args[1];
        String testSuitePath = args[2];

        Collection<File> testSuites = FileUtils.listFiles(
            Paths.get(testSuitePath).toFile(), 
            new RegexFileFilter("^(.*?)\\.spt$"), 
            DirectoryFileFilter.DIRECTORY
        );

        try(Spoofax spoofax = new Spoofax(new SPTModule())) {
            CLIUtils cli = new CLIUtils(spoofax);

            FileObject sptDir = spoofax.resourceService.resolve(sptPath);
            ILanguageImpl spt = spoofax.languageDiscoveryService.languageFromDirectory(sptDir);

            FileObject languageUnderTestDir = spoofax.resourceService.resolve(languageUnderTestPath);
            ILanguageImpl languageUnderTest = spoofax.languageDiscoveryService.languageFromDirectory(languageUnderTestDir);

            IProject sptProject = cli.getOrCreateProject(sptDir);
            IProject lutProject = cli.getOrCreateProject(languageUnderTestDir);

            final Injector injector = spoofax.injector;
            final ISpoofaxTestCaseExtractor extractor = injector.getInstance(SpoofaxTestCaseExtractor.class);

            Collection<TestCase> results = new ArrayList<TestCase>();
            Map<String, List<String>> index = new HashMap<String, List<String>>();

            for (File testSuite : testSuites) {
                FileObject testSuiteFile = spoofax.resourceService.resolve(testSuite.getAbsolutePath());
                String text = spoofax.sourceTextService.text(testSuiteFile);
                ISpoofaxInputUnit testSuiteUnit = spoofax.unitService.inputUnit(testSuiteFile, text, spt, spt);

                ISpoofaxTestCaseExtractionResult result = extractor.extract(testSuiteUnit, sptProject);

                List<String> descriptions = new ArrayList<String>();
                if (result.isSuccessful()) {
                    System.out.println("[SUCCESS] extracted tests from: " + testSuiteFile.getPublicURIString());
                    for (ITestCase test : result.getTests()) {
                        descriptions.add(test.getDescription());
                        StringBuilder sb = new StringBuilder();
                        for (FragmentPiece piece : test.getFragment().getText()) {
                            sb.append(piece.text);
                        }
                        String fragmentText = sb.toString();
                        ISpoofaxInputUnit input = spoofax.unitService.inputUnit(fragmentText, languageUnderTest, languageUnderTest);
                        ISpoofaxParseUnit parseUnit = spoofax.syntaxService.parse(input);

                        File tmpFile = File.createTempFile("fragment", ".tmp");
                        FileWriter writer = new FileWriter(tmpFile);
                        writer.write(fragmentText);
                        writer.close();

                        FileObject fragmentFile = spoofax.resourceService.resolve(tmpFile.getAbsolutePath());

                        IContext context = spoofax.contextService.get(fragmentFile, lutProject, languageUnderTest);

                        IStrategoCommon strategoCommon = injector.getInstance(IStrategoCommon.class);
                        IStrategoTerm transformed;
                        try(IClosableLock lock = context.read()) {
                            transformed = strategoCommon.invoke(parseUnit.input().langImpl(),
                                context, parseUnit.ast(), "desugar-before-analysis");
                        }                        

                        String fragmentAst = transformed.toString();
                        results.add(new TestCase(testSuiteFile.getPublicURIString(), test.getDescription(), fragmentText, fragmentAst));
                    }
                    index.put(testSuite.getAbsolutePath(), descriptions);
                } else {
                    System.out.println("[FAILURE] failed to extract tests from: " + testSuiteFile.getPublicURIString());
                    for (IMessage message : result.getAllMessages()) {
                        System.out.println(message.toString());
                    }
                }
            }
            
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.writeValue(new File("output/extract_tests_output.yml"), results);
            objectMapper.writeValue(new File("output/extract_tests_index.yml"), index);
        }
    }
}

class TestCase {
    public String path;
    public String description;
    public String text;
    public String ast;

    public TestCase(String path, String description, String text, String ast) {
        this.path = path;
        this.description = description;
        this.text = text;
        this.ast = ast;
    }
}
