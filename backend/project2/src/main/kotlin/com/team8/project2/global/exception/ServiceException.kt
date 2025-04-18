package com.team8.project2.global.exception

import com.team8.project2.global.dto.RsData

class ServiceException(code: String, message: String) : RuntimeException(message) {
    private val rsData: RsData<*> = RsData<Unit>(code, message)

    val code: String
        get() = rsData.code

    val msg: String
        get() = rsData.msg

    val statusCode: Int
        get() = rsData.statusCode
}
