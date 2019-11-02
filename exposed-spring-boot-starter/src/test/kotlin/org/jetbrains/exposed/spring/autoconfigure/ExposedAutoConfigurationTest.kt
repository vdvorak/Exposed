package org.jetbrains.exposed.spring.autoconfigure

import org.jetbrains.exposed.spring.DatabaseInitializer
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.spring.tables.TestTable
import org.jetbrains.exposed.sql.selectAll
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [org.jetbrains.exposed.spring.Application::class],
        properties = ["spring.datasource.url=jdbc:h2:mem:test", "spring.datasource.driver-class-name=org.h2.Driver"])
open class ExposedAutoConfigurationTest {

    @Autowired(required = false)
    private var springTransactionManager: SpringTransactionManager? = null

    @Autowired(required = false)
    private var databaseInitializer: DatabaseInitializer? = null

    @Test
    fun `should initialize the database connection`() {
        Assert.assertNotNull(springTransactionManager)
    }

    @Test
    fun `should not create schema`() {
        Assert.assertNull(databaseInitializer)
    }
}

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [org.jetbrains.exposed.spring.Application::class],
        properties = ["spring.datasource.url=jdbc:h2:mem:test", "spring.datasource.driver-class-name=org.h2.Driver","spring.exposed.generate-ddl=true"])
open class ExposedAutoConfigurationTestAutoGenerateDDL {

    @Autowired(required = false)
    private var springTransactionManager: SpringTransactionManager? = null

    @Autowired
    private lateinit var asyncService: AsyncExposedService

    @Test
    fun `should initialize the database connection`() {
        Assert.assertNotNull(springTransactionManager)
    }

    @Test @Transactional
    open fun `should create schema`() {
        Assert.assertEquals(0, TestTable.selectAll().count())
    }

    @Test @Transactional @Ignore
    open fun `async service should be initialized properly`() {
        Assert.assertEquals(0, asyncService.allTestData().size)
    }

    @Test @Transactional @Ignore
    open fun `async service should be initialized properly with completableFuture`() {
        Assert.assertEquals(0, asyncService.allTestDataAsync().join().size)
    }
}

// See https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#scheduling-annotation-support
@Transactional
@Service
@Async // if not put then allTestData() has no error. In all cases allTestDataAsync will have error
// Issue comes from TransactionAspectSupport, implementation changed between spring version 5.1.X and 5.2.0
open class AsyncExposedService {

    // if not using @EnableAsync, this method passes the test which should not
    open fun allTestData() = TestTable.selectAll().toList()

    // you need to put open otherwise @Transactional is not applied since spring plugin not applied (similar to maven kotlin plugin)
    open fun allTestDataAsync() = CompletableFuture.completedFuture(TestTable.selectAll().toList())
}
