package net.corda.gradle.jarfilter

import net.corda.gradle.unwanted.HasUnwantedFun
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFailsWith

class DeleteObjectTest {
    companion object {
        private const val OBJECT_CLASS = "net.corda.gradle.HasObjects"
        private const val UNWANTED_OBJ_METHOD = "getUnwantedObj"
        private const val UNWANTED_OBJ_FIELD = "unwantedObj"
        private const val UNWANTED_FUN_METHOD = "unwantedFun"

        private lateinit var testProject: JarFilterProject
        private lateinit var sourceClasses: List<String>
        private lateinit var filteredClasses: List<String>

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir testProjectDir: Path) {
            testProject = JarFilterProject(testProjectDir, "delete-object").build()
            sourceClasses = testProject.sourceJar.getClassNames(OBJECT_CLASS)
            filteredClasses = testProject.filteredJar.getClassNames(OBJECT_CLASS)
        }
    }

    @Test
    fun deletedClasses() {
        assertThat(sourceClasses).contains(OBJECT_CLASS)
        assertThat(filteredClasses).containsExactly(OBJECT_CLASS)
    }

    @Test
    fun deleteObject() {
        assertThat(sourceClasses).anyMatch { it.contains("\$unwantedObj\$") }

        classLoaderFor(testProject.sourceJar).use { cl ->
            cl.load<Any>(OBJECT_CLASS).apply {
                getDeclaredMethod(UNWANTED_OBJ_METHOD).also { method ->
                    (method.invoke(null) as HasUnwantedFun).also { obj ->
                        assertEquals(MESSAGE, obj.unwantedFun(MESSAGE))
                    }
                }
                getDeclaredField(UNWANTED_OBJ_FIELD).also { field ->
                    assertFalse(field.isAccessible)
                }
            }
        }

        classLoaderFor(testProject.filteredJar).use { cl ->
            cl.load<Any>(OBJECT_CLASS).apply {
                assertFailsWith<NoSuchMethodException> { getDeclaredMethod(UNWANTED_OBJ_METHOD) }
                assertFailsWith<NoSuchFieldException> { getDeclaredField(UNWANTED_OBJ_FIELD) }
            }
        }
    }

    @Test
    fun deleteFunctionWithObject() {
        assertThat(sourceClasses).anyMatch { it.contains("\$unwantedFun\$") }

        classLoaderFor(testProject.sourceJar).use { cl ->
            cl.load<Any>(OBJECT_CLASS).apply {
                getDeclaredMethod(UNWANTED_FUN_METHOD).also { method ->
                    assertEquals("<default-value>", method.invoke(null))
                }
            }
        }

        classLoaderFor(testProject.filteredJar).use { cl ->
            cl.load<Any>(OBJECT_CLASS).apply {
                assertFailsWith<NoSuchMethodException> { getDeclaredMethod(UNWANTED_FUN_METHOD) }
            }
        }
    }
}