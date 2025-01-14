package beam.agentsim.infrastructure

import beam.agentsim.agents.vehicles.VehicleManager
import beam.agentsim.infrastructure.charging.ChargingPointType.CustomChargingPoint
import beam.agentsim.infrastructure.charging.ElectricCurrentType.DC
import beam.agentsim.infrastructure.parking.ParkingType.Residential
import beam.agentsim.infrastructure.parking.ParkingZoneFileUtilsSpec.PositiveTestData
import beam.agentsim.infrastructure.parking.{LinkLevelOperations, ParkingZone, ParkingZoneFileUtils}
import beam.agentsim.infrastructure.taz.{TAZ, TAZTreeMap}
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.Link
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter
import scala.util.Random

/**
  * @author Dmitry Openkov
  */
class HierarchicalParkingManagerUtilSpec extends AnyWordSpec with Matchers {
  "HierarchicalParkingManager" when {
    "creates taz parking zones out of link parking zones" should {
      "produce correct zones" in new PositiveTestData {
        val ParkingZoneFileUtils.ParkingLoadingAccumulator(linkZones, linkTree, totalRows, failedRows) =
          ParkingZoneFileUtils
            .fromIterator[Link](linkLevelData, vehicleManagerId = VehicleManager.privateVehicleManager.managerId)
        val linkToTazMapping: Map[Id[Link], Id[TAZ]] = HashMap(
          Id.createLinkId(49577) -> Id.create(100026, classOf[TAZ]),
          Id.createLinkId(83658) -> Id.create(100026, classOf[TAZ]),
          Id.createLinkId(83661) -> Id.create(100026, classOf[TAZ]),
          Id.createLinkId(83663) -> Id.create(100100, classOf[TAZ]),
        )
        private val (tazZones, linkToTaz) =
          HierarchicalParkingManager.convertToTazParkingZones(linkZones.toArray, linkToTazMapping)

        println(tazZones.mkString("Array(", ", ", ")"))
        tazZones.length should be(9)
        val joinZone: Option[ParkingZone[TAZ]] = tazZones.find(
          zone =>
            zone.geoId.toString == "100026" && zone.parkingType == Residential && zone.chargingPointType.contains(
              CustomChargingPoint("dcfast", 50.0, DC)
          )
        )

        joinZone shouldBe defined
        joinZone.get.maxStalls should be(1100)
        tazZones.count(_.geoId.toString == "100100") should be(2)
      }
    }
    "creates taz link quad tree mapping" should {
      "correct quad tree" in {
        val network =
          NetworkUtilsExtensions.readNetwork("test/test-resources/beam/physsim/beamville-network-output.xml")
        val totalNumberOfLinks = network.getLinks.size()
        val tazTreeMap = TAZTreeMap.fromCsv("test/input/beamville/taz-centers.csv")
        val totalNumberOfTAZes = tazTreeMap.tazQuadTree.size()

        val linkToTAZMapping: Map[Link, TAZ] = LinkLevelOperations.getLinkToTazMapping(network, tazTreeMap)
        linkToTAZMapping.size should be(totalNumberOfLinks)
        linkToTAZMapping.values.toSet.size should be(totalNumberOfTAZes)

        val tazToLinkQuads = HierarchicalParkingManager.createTazLinkQuadTreeMapping(linkToTAZMapping)
        tazToLinkQuads.values.map(_.size()).sum should be(totalNumberOfLinks)

        //get certain TAZ links and validate that it contains the right ones.
        val myTazId: Id[TAZ] = Id.create(1, classOf[TAZ])
        val myZonesTree = tazToLinkQuads(myTazId)
        myZonesTree.values().size() should be(130)
        myZonesTree.values().asScala.map(_.getId.toString).toList should contain allElementsOf List(
          "2",
          "3",
          "6",
          "7",
          "322",
          "323"
        )
      }
      "collapsing parking zones " should {
        "produce a simplified structure" in {
          val (parkingZones, _) =
            ParkingZoneFileUtils
              .fromFile[TAZ](
                "test/test-resources/beam/agentsim/infrastructure/taz-parking-similar-zones.csv",
                new Random(777934L),
                vehicleManagerId = Id.create("default", classOf[VehicleManager])
              )
          parkingZones should have length 3648
          val zones205 = parkingZones.filter(_.geoId.toString == "205")
          zones205 should have length 20
          val collapsed = HierarchicalParkingManager.collapse(parkingZones)
          val collapsedZones205 = collapsed.filter(_.geoId.toString == "205")
          collapsedZones205 should have length 11
          collapsed should have length 2236
        }
      }
    }
    "collapse parking zones" should {
      "produce a simplified structure" in {
        val (parkingZones, _) =
          ParkingZoneFileUtils
            .fromFile[TAZ](
              "test/test-resources/beam/agentsim/infrastructure/taz-parking-similar-zones.csv",
              new Random(777934L),
              vehicleManagerId = Id.create("default", classOf[VehicleManager])
            )
        parkingZones should have length 3648
        val zones205 = parkingZones.filter(_.geoId.toString == "205")
        zones205 should have length 20
        val collapsed = HierarchicalParkingManager.collapse(parkingZones)
        val collapsedZones205 = collapsed.filter(_.geoId.toString == "205")
        collapsedZones205 should have length 11
        collapsed should have length 2236
      }
    }
  }
}
