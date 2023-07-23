package mb.nabl2.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalyzerFacet;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.FacetContribution;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.IFragment.FragmentPiece;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.AnalysisFacet;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResult;
import org.metaborg.spoofax.core.analysis.constraint.SingleFileConstraintAnalyzer;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Injector;

import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.extract.SpoofaxTestCaseExtractor;
import org.metaborg.spt.core.extract.SpoofaxTracingFragmentBuilder;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.spt.core.extract.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;
import org.metaborg.spt.core.extract.SpoofaxTestCaseBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;

public class AnalysisRun {
    public static void main(String[] args) throws Throwable {
        String sptPath = args[0];
        String languageUnderTestPath = args[1];
        String testSuitePath = args[2];

        try(Spoofax spoofax = new Spoofax(new SPTModule())) {
            CLIUtils cli = new CLIUtils(spoofax);

            FileObject sptDir = spoofax.resourceService.resolve(sptPath);
            ILanguageImpl spt = spoofax.languageDiscoveryService.languageFromDirectory(sptDir);

            FileObject languageUnderTestDir = spoofax.resourceService.resolve(languageUnderTestPath);
            ILanguageImpl languageUnderTest = spoofax.languageDiscoveryService.languageFromDirectory(languageUnderTestDir);

            IProject sptProject = cli.getOrCreateProject(sptDir);

            final Injector injector = spoofax.injector;
            final ISpoofaxTestCaseExtractor extractor = injector.getInstance(SpoofaxTestCaseExtractor.class);
            // ISpoofaxFragmentBuilder fragmentBuilder = new SpoofaxTracingFragmentBuilder(spoofax.tracingService);
            // Set<ISpoofaxTestExpectationProvider> expectationProviders = new HashSet<ISpoofaxTestExpectationProvider>();
            // ISpoofaxTestCaseBuilder testCaseBuilder = new SpoofaxTestCaseBuilder(expectationProviders, fragmentBuilder, spoofax.tracingService);

            // ISpoofaxTestCaseExtractor extractor = new SpoofaxTestCaseExtractor(
            //     spoofax.syntaxService, spoofax.analysisService, spoofax.contextService, testCaseBuilder);

            Set<File> testSuites = Stream.of(new File(testSuitePath).listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toSet());

            Collection<TestCase> results = new ArrayList<TestCase>();

            for (File testSuite : testSuites) {
                FileObject testSuiteFile = spoofax.resourceService.resolve(path);
                String text = spoofax.sourceTextService.text(testSuiteFile);
                ISpoofaxInputUnit testSuiteUnit = spoofax.unitService.inputUnit(testSuiteFile, text, spt, spt);

                ISpoofaxTestCaseExtractionResult result = extractor.extract(testSuiteUnit, sptProject);

                if (result.isSuccessful()) {
                    System.out.println("[SUCCESS] extracted tests from: " + testSuiteFile.getPublicURIString());
                    for (ITestCase test : result.getTests()) {
                        StringBuilder sb = new StringBuilder();
                        for (FragmentPiece piece : test.getFragment().getText()) {
                            sb.append(piece.text);
                        }
                        String fragmentText = sb.toString();
                        ISpoofaxInputUnit input = spoofax.unitService.inputUnit(fragmentText, languageUnderTest, languageUnderTest);
                        ISpoofaxParseUnit parseUnit = spoofax.syntaxService.parse(input);
                        String fragmentAst = parseUnit.ast().toString();
                        results.add(new TestCase(testSuiteFile.getPublicURIString(), test.getDescription(), fragmentText, fragmentAst));
                    }
                } else {
                    System.out.println("[FAILURE] failed to extract tests from: " + testSuiteFile.getPublicURIString());
                    for (IMessage message : result.getAllMessages()) {
                        System.out.println(message.toString());
                    }
                }
            }
            
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.writeValue(new File("output.yml"), results);
            // ISpoofaxParseUnit parseUnit = spoofax.syntaxService.parse(input);
            // IContext context = spoofax.contextService.get(fileToParse, project, language);

            // AnalyzerFacet<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate> facet = language.facet(AnalyzerFacet.class);
            // SingleFileConstraintAnalyzer analyzer = (SingleFileConstraintAnalyzer) facet.analyzer;
            // FacetContribution<AnalysisFacet> facetContribution = language.facetContribution(AnalysisFacet.class);

            // System.out.println(facetContribution.facet.strategyName);

            // ISpoofaxAnalyzeResult result;
            // try(IClosableLock lock = context.write()) {
            //     result = spoofax.analysisService.analyze(parseUnit, context);
            // }
            // ISpoofaxAnalyzeUnit analyzeUnit = result.result();

            // System.out.println(analyzeUnit.ast().toString());
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
