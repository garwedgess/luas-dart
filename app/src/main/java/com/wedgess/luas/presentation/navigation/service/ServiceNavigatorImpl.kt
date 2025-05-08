package com.wedgess.luas.presentation.navigation.service

import com.wedgess.luas.MainActivity
import com.wedgess.luas.domain.navigation.ServiceNavigator

class ServiceNavigatorImpl : ServiceNavigator {
    override fun mainActivityClass(): Class<*> = MainActivity::class.java
}
