package com.boomesh.security.common

import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.*

class DateUtilTest {

    @Mock
    private lateinit var calendarFactory: (() -> Calendar)

    private lateinit var dateUtil: DateUtil

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        dateUtil = DateUtil(calendarFactory)
    }

    @After
    fun tearDown() {
        Mockito.reset(calendarFactory)
    }


    @Test
    fun `calendar will return current time`() {
        val calendar = Calendar.getInstance()
        `when`(calendarFactory()).thenReturn(calendar)
        Assertions.assertThat(dateUtil.now).isEqualTo(calendar)
    }

    @Test
    fun `adding one year to calendar`() {
        val now = Calendar.getInstance()
        val nextYear = Calendar.getInstance()
        dateUtil.addYearsTo(nextYear, 1)
        Assertions.assertThat(now.get(Calendar.YEAR)).isEqualTo(nextYear.get(Calendar.YEAR) - 1)
    }
}