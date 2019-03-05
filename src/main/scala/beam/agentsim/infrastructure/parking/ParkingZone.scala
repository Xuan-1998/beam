package beam.agentsim.infrastructure.parking

import scala.language.higherKinds

import cats.Eval

import beam.agentsim.infrastructure.charging.ChargingPointType

/**
  * stores the number of stalls in use for a zone of parking stalls with a common set of attributes
  * @param parkingZoneId the Id of this Zone, which directly corresponds to the Array index of this used in the ParkingZoneSearch Array[ParkingZone]
  * @param stallsAvailable a (mutable) count of stalls free, which is mutated to track the current state of stalls in a way that is logically similar to a semiphore
  * @param ChargingPointType if this stall has charging, this is the type of charging
  * @param pricingModel if this stall has pricing, this is the type of pricing
  */
class ParkingZone(
                   val parkingZoneId: Int,
                   var stallsAvailable: Int,
                   val maxStalls: Int,
                   val chargingPointType: Option[ChargingPointType],
                   val pricingModel: Option[PricingModel]
) {
  override def toString: String = {
    val chargeString = chargingPointType match {
      case None    => "chargingType = None"
      case Some(c) => s" chargingType = $c"
    }
    val pricingString = pricingModel match {
      case None    => "pricingModel = None"
      case Some(p) => s" pricingModel = $p"
    }
    s"ParkingZone(parkingZoneId = $parkingZoneId, numStalls = $stallsAvailable, $chargeString, $pricingString)"
  }
}

object ParkingZone {

  val DefaultParkingZoneId: Int = -1

  val DefaultParkingZone = ParkingZone(DefaultParkingZoneId, Int.MaxValue, None, None)

  /**
    * creates a new StallValues object
    * @param chargingType if this stall has charging, this is the type of charging
    * @param pricingModel if this stall has pricing, this is the type of pricing
    * @return a new StallValues object
    */
  def apply(
             parkingZoneId: Int,
             numStalls: Int = 0,
             chargingType: Option[ChargingPointType] = None,
             pricingModel: Option[PricingModel] = None,
  ): ParkingZone = new ParkingZone(parkingZoneId, numStalls, numStalls, chargingType, pricingModel)

  /**
    * increment the count of stalls in use
    * @param parkingZone the object to increment
    * @return True|False (representing success) wrapped in an effect type
    */
  def releaseStall(parkingZone: ParkingZone): Eval[Boolean] =
    Eval.later {
      if (parkingZone.parkingZoneId == DefaultParkingZoneId) {
        // this zone does not exist in memory but it has infinitely many stalls to release
        true
      } else if (parkingZone.stallsAvailable + 1 == parkingZone.maxStalls) {
//        log.debug(s"Attempting to release a parking stall when ParkingZone is already full.")
        false
      } else {
        parkingZone.stallsAvailable += 1
        true
      }
    }

  /**
    * decrement the count of stalls in use. doesn't allow negative-values (fails silently)
    * @param parkingZone the object to increment
    * @return True|False (representing success) wrapped in an effect type
    */
  def claimStall(parkingZone: ParkingZone): Eval[Boolean] =
    Eval.later {
      if (parkingZone.parkingZoneId == DefaultParkingZoneId) {
        // this zone does not exist in memory but it has infinitely many stalls to release
        true
      } else if (parkingZone.stallsAvailable - 1 >= 0) {
        parkingZone.stallsAvailable -= 1
        true
      } else {
        // log debug that we tried to claim a stall when there were no free stalls
        false
      }
    }
}
