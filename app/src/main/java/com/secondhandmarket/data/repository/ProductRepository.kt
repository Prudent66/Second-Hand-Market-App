package com.secondhandmarket.data.repository

import com.secondhandmarket.data.models.Product
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor() {

    // Mock data for now. In a real app, this would call the API or Firebase.
    // Images use deterministic Picsum seeds so every item has a stable photo
    // that survives app restarts (and is cached by Coil after first load).
    private val mockProducts = mutableListOf(
        Product(
            id = "1",
            vendorId = "v1",
            title = "HP Laptop 15-inch",
            description = "Gently used HP laptop with i5 processor, 8GB RAM and 256GB SSD.",
            price = 250000.0,
            imageUrl = "https://picsum.photos/seed/hp-laptop-15-inch/400/400",
            category = "Electronics",
            condition = "Good",
            createdAt = System.currentTimeMillis()
        ),
        Product(
            id = "2",
            vendorId = "v2",
            title = "Samsung Galaxy S21",
            description = "Original Samsung phone, 128GB, minor scratches on the back.",
            price = 350000.0,
            imageUrl = "https://picsum.photos/seed/samsung-galaxy-s21/400/400",
            category = "Phones",
            condition = "Fair",
            createdAt = System.currentTimeMillis() - 100000
        ),
        Product(
            id = "3",
            vendorId = "v3",
            title = "Wooden Wardrobe",
            description = "Solid wood wardrobe, very spacious with a full-length mirror.",
            price = 80000.0,
            imageUrl = "https://picsum.photos/seed/wooden-wardrobe/400/400",
            category = "Furniture",
            condition = "Like New",
            createdAt = System.currentTimeMillis() - 200000
        ),
        Product(
            id = "4",
            vendorId = "v2",
            title = "Vintage Denim Jacket",
            description = "Classic blue denim jacket, size M, barely worn.",
            price = 15000.0,
            imageUrl = "https://picsum.photos/seed/vintage-denim-jacket/400/400",
            category = "Fashion",
            condition = "Good",
            createdAt = System.currentTimeMillis() - 300000
        ),
        Product(
            id = "5",
            vendorId = "v4",
            title = "iPhone 13 Pro",
            description = "256GB, graphite, battery health 91 percent with box and cable.",
            price = 620000.0,
            imageUrl = "https://picsum.photos/seed/iphone-13-pro/400/400",
            category = "Phones",
            condition = "Like New",
            createdAt = System.currentTimeMillis() - 400000
        ),
        Product(
            id = "6",
            vendorId = "v3",
            title = "Ergonomic Office Chair",
            description = "Mesh back office chair with adjustable arms and lumbar support.",
            price = 45000.0,
            imageUrl = "https://picsum.photos/seed/ergonomic-office-chair/400/400",
            category = "Furniture",
            condition = "Good",
            createdAt = System.currentTimeMillis() - 500000
        ),
        Product(
            id = "7",
            vendorId = "v5",
            title = "PlayStation 5 Console",
            description = "Disc edition with two controllers and three games.",
            price = 480000.0,
            imageUrl = "https://picsum.photos/seed/playstation-5-console/400/400",
            category = "Electronics",
            condition = "Like New",
            createdAt = System.currentTimeMillis() - 600000
        ),
        Product(
            id = "8",
            vendorId = "v2",
            title = "Ankara Two-Piece Set",
            description = "Handmade Ankara set, tailored fit, size L.",
            price = 22000.0,
            imageUrl = "https://picsum.photos/seed/ankara-two-piece-set/400/400",
            category = "Fashion",
            condition = "New",
            createdAt = System.currentTimeMillis() - 700000
        ),
        Product(
            id = "9",
            vendorId = "v3",
            title = "Study Desk with Shelves",
            description = "Compact study desk with two shelves, easy to dismantle.",
            price = 38000.0,
            imageUrl = "https://picsum.photos/seed/study-desk-with-shelves/400/400",
            category = "Furniture",
            condition = "Good",
            createdAt = System.currentTimeMillis() - 800000
        ),
        Product(
            id = "10",
            vendorId = "v1",
            title = "MacBook Air M1",
            description = "8GB RAM, 256GB SSD, space grey, comes with original charger.",
            price = 750000.0,
            imageUrl = "https://picsum.photos/seed/macbook-air-m1/400/400",
            category = "Electronics",
            condition = "Like New",
            createdAt = System.currentTimeMillis() - 900000
        )
    )

    suspend fun getProducts(): List<Product> {
        kotlinx.coroutines.delay(400)
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
        mockProducts.add(0, newProduct)
        return newProduct
    }
}
