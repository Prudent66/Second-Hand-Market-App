package com.secondhandmarket.data.models

data class Product(
    val id: String,
    val vendorId: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val condition: String,
    val createdAt: Long
)