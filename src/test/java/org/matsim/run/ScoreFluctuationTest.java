package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;
import java.util.Random;

/**
 * @author gthunig on 16.10.2018
 */
public class ScoreFluctuationTest {

    private static final Logger log = Logger.getLogger( RunBerlinScenarioTest.class ) ;

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testFluctuatuingScore() {

        //Score:  114.73387586329103 from ihab calculated score, no sample

        //run fractionOfIteration(1) lastIteration(1)
        //Score1: 113.75741996255046
        //Score2: 113.75742175860532

        //run fractionOfIteration(1) lastIteration(1) numberOfThreads(1)
        //Score1: 113.80144702530018
        //Score2: 113.8014470331611

        //run fractionOfIteration(1) lastIteration(1) numberOfThreads(1) plans_noPt
        //Score1: 111.8452974103275
        //Score2: 111.8452974103275

        //run fractionOfIteration(1) lastIteration(1) numberOfThreads(6) plans_noPt
        //Score1: 111.81562881445556
        //Score2: 111.81562881445556

        double score1 = run1pctAndReturnScore();
        double score2 = run1pctAndReturnScore();

        System.out.println("Score1: " + score1);
        System.out.println("Score2: " + score2);
        Assert.assertEquals(score1, score2, MatsimTestUtils.EPSILON);

    }

    private double run1pctAndReturnScore() {
        try {
            String configFilename = "scenarios/berlin-v5.2-1pct/input/berlin-v5.2-1pct.config.xml";
            RunBerlinScenario berlin = new RunBerlinScenario( configFilename, "overridingConfig.xml" ) ;

            Config config =  berlin.prepareConfig();
            int lastIteration = 1;
            config.controler().setLastIteration(lastIteration);
            config.global().setNumberOfThreads(6);
            config.strategy().setFractionOfIterationsToDisableInnovation(1);//to enable innovative strategies
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
            config.controler().setOutputDirectory( utils.getOutputDirectory() );

//            Scenario scenario = berlin.prepareScenario() ;
//            downsample( scenario.getPopulation().getPersons(), 0.01 );

            berlin.run() ;

            return berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average).get(lastIteration);

        } catch ( Exception ee ) {
            throw new RuntimeException(ee) ;
        }
    }

    private static void downsample( final Map map, final double sample ) {
        final Random rnd = new Random(12345);
        log.warn( "map size before=" + map.size() ) ;
        map.values().removeIf( person -> rnd.nextDouble()>sample);
        log.warn( "map size after=" + map.size() ) ;
    }
}
