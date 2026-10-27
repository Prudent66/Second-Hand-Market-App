package com.secondhandmarket.data.repository

import com.secondhandmarket.data.models.Product
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor() {

    // Mock data for now. In a real app, this would call the API or Firebase.
    private val mockProducts = mutableListOf(
        Product(
            id = "1",
            vendorId = "v1",
            title = "HP Laptop 15-inch",
            description = "Gently used HP laptop with i5 processor.",
            price = 250000.0,
            imageUrl = "",
            category = "Electronics",
            condition = "Good",
            createdAt = System.currentTimeMillis()
        ),
        Product(
            id = "2",
            vendorId = "v2",
            title = "Samsung Galaxy S21",
            description = "Original Samsung phone, minor scratches on back.",
            price = 350000.0,
            imageUrl = "",
            category = "Phones",
            condition = "Fair",
            createdAt = System.currentTimeMillis() - 100000
        ),
        Product(
            id = "3",
            vendorId = "v3",
            title = "Wooden Wardrobe",
            description = "Solid wood wardrobe, very spacious.",
            price = 80000.0,
            imageUrl = "",
            category = "Furniture",
            condition = "Like New",
            createdAt = System.currentTimeMillis() - 200000
        )
    )

    suspend fun getProducts(): List<Product> {
        kotlinx.coroutines.delay(500)
        return mockProducts.toList()
    }

    suspend fun getProductById(id: String): Product? {
        return mockProducts.find { it.id == id }
    }

    suspend fun createProduct(
        vendorId: String,
        title: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String,
        condition: String
    ): Product {
        val newProduct = Product(
            id = (mockProducts.size + 1).toString(),
            vendorId = vendorId,
            title = title,
            description = description,
            price = price,
            imageUrl = imageUrl,
            category = category,
            condition = condition,
            createdAt = System.currentTimeMillis()
        )
        mockProducts.add(newProduct)
        return newProduct
    }
}