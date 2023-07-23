package mb.nabl2.benchmark;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResult;
import org.metaborg.spoofax.core.context.constraint.IConstraintContext;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
@State(Scope.Thread)
public class AnalysisBenchmark {
    @State(Scope.Thread)
    public static class AnalysisState {
        @Param({}) public String languagePath;
        @Param({}) public String programPath;
    
        private Spoofax spoofax;
        public ISpoofaxAnalysisService analysisService;
        public ISpoofaxParseUnit parseUnit;
        public IContext context;

        @Setup(Level.Trial)
        public void doSetup() throws MetaborgException {
            spoofax = new Spoofax();
            try {
                CLIUtils cli = new CLIUtils(spoofax);
                FileObject languageDir = spoofax.resourceService.resolve(this.languagePath);
                IProject project = cli.getOrCreateProject(languageDir);
                ILanguageImpl language = spoofax.languageDiscoveryService.languageFromDirectory(languageDir);
                FileObject fileToParse = spoofax.resourceService.resolve(this.programPath);
                String text = spoofax.sourceTextService.text(fileToParse);
                ISpoofaxInputUnit input = spoofax.unitService.inputUnit(fileToParse, text, language, language);

                analysisService = spoofax.analysisService;
                parseUnit = spoofax.syntaxService.parse(input);
                context = spoofax.contextService.get(fileToParse, project, language);
            } catch(Throwable t) {
                doTearDown();
                throw new RuntimeException(t);
            }
        }

        @TearDown(Level.Trial)
        public void doTearDown() {
            spoofax.close();
        }
    }

    @Benchmark
    public void runBenchmark(Blackhole blackhole, AnalysisState state) throws Exception {
        try {
            ISpoofaxAnalyzeResult result;
            IConstraintContext constraintContext;
            try(IClosableLock lock = state.context.write()) {
                constraintContext = (IConstraintContext) state.context;
                constraintContext.clear();
                result = state.analysisService.analyze(state.parseUnit, state.context);
            }
            blackhole.consume(result);
        } catch (Throwable t) {
            state.doTearDown();
            throw new RuntimeException(t);
        }
        
    }
}
