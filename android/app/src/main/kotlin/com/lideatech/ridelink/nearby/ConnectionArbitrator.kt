package com.lideatech.ridelink.nearby

object ConnectionArbitrator {
    fun shouldInitiate(localToken: String, remoteToken: String): Boolean =
        localToken < remoteToken
}
