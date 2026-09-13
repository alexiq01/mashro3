package com.example.subscriptions.model

data class Subscription(val name:String="",val profile:String="",val start:String="",val expiry:String="",val timeUsed:String="غير متوفر",val timeLeft:String="غير متوفر",val trafficUsed:String="غير متوفر",val trafficLeft:String="غير متوفر",val expiryMillis:Long=0L)
