package com.disc.hdfccard


sealed class MyResult {
    data class Success(val msg:String): MyResult()
    data class Error(val msg:String): MyResult()
}