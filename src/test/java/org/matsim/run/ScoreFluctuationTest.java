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
        //run fractionOfIteration 1 lastIteration(3)
        //Score1: 114.65161732321907
        //Score2: 114.65161732321907

        //run fractionOfIteration 1 lastIteration(1)
        //Score1: 114.65161732321907
        //Score2: 114.65161732321907

        //run1 no sample lastIteration(1)
        //Score1: 114.65161732321907
        //Score2: 114.65161732321907

        //run1 no sample lastIteration(0)
        //Score:  114.65161732321907 first calculated score, no sample
        //run2
        //Score1: 114.65161732321907 no sample
        //Score2: 114.65161732321907 no sample

        //run1 with sample 0.01
        //Score1: 115.6444324032385 sample 0.01
        //Score2: 115.6444324032385 sample 0.01

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

            Config config =  berlin.prepareConfig() ;
            config.controler().setLastIteration(3);
            config.strategy().setFractionOfIterationsToDisableInnovation(1);//to enable innovative strategies
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
            config.controler().setOutputDirectory( utils.getOutputDirectory() );

//            Scenario scenario = berlin.prepareScenario() ;
//            downsample( scenario.getPopulation().getPersons(), 0.01 );

            berlin.run() ;

            return berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average).get(0);

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
