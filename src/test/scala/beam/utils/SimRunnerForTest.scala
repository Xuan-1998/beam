package beam.utils
import java.io.File

import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.router.gtfs.FareCalculator
import beam.router.osm.TollCalculator
import beam.sim.{BeamHelper, BeamScenario, BeamServices, BeamServicesImpl}
import beam.sim.common.GeoUtils
import beam.sim.config.{BeamConfig, MatSimBeamConfigBuilder}
import com.google.inject.Injector
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting
import org.matsim.core.scenario.MutableScenario
import org.scalatest.{BeforeAndAfterAll, Suite}

trait SimRunnerForTest extends BeamHelper with BeforeAndAfterAll { this: Suite =>
  def config: com.typesafe.config.Config
  def basePath: String = new File("").getAbsolutePath
  def testOutputDir: String = TestConfigUtils.testOutputDir
  def outputDirPath: String

  // Next things are pretty cheap in initialization, so let it be non-lazy
  val beamCfg = BeamConfig(config)
  val matsimConfig = new MatSimBeamConfigBuilder(config).buildMatSimConf()
  matsimConfig.controler.setOutputDirectory(outputDirPath)
  matsimConfig.controler.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles)

  var beamScenario: BeamScenario = _
  var scenario: MutableScenario = _
  var injector: Injector = _
  var services: BeamServices = _

  def fareCalculator: FareCalculator = injector.getInstance(classOf[FareCalculator])
  def tollCalculator: TollCalculator = injector.getInstance(classOf[TollCalculator])
  def geoUtil: GeoUtils = injector.getInstance(classOf[GeoUtils])
  def networkHelper: NetworkHelper = injector.getInstance(classOf[NetworkHelper])

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    beamScenario = loadScenario(beamCfg)
    scenario = buildScenarioFromMatsimConfig(matsimConfig, beamScenario)
    injector = buildInjector(config, scenario, beamScenario)
    services = new BeamServicesImpl(injector)
    services.modeChoiceCalculatorFactory = ModeChoiceCalculator(
      services.beamConfig.beam.agentsim.agents.modalBehaviors.modeChoiceClass,
      services
    )
  }

  override protected def afterAll(): Unit = {
    beamScenario = null
    scenario = null
    injector = null
    super.afterAll()
  }

}
