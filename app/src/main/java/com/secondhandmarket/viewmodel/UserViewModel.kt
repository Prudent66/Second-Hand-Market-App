package com.secondhandmarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondhandmarket.data.models.Product
import com.secondhandmarket.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                // In a real app, this would fetch from API/Firebase
                // For now, we simulate loading or use local cache
                val fetchedProducts = productRepository.getProducts()
                _products.value = fetchedProducts
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error state if needed
            }
        }
    }
}