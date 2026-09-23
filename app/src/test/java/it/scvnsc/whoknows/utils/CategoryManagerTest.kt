package it.scvnsc.whoknows.utils

import it.scvnsc.whoknows.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryManagerTest {

    @Before
    fun clear() {
        CategoryManager.categories.clear()
    }

    @Test
    fun `builds a name to id map from the api categories`() {
        CategoryManager.buildCategoriesMap(
            listOf(Category(9, "General Knowledge"), Category(18, "Science: Computers"))
        )

        assertEquals(18, CategoryManager.categories["Science: Computers"])
        assertEquals(9, CategoryManager.categories["General Knowledge"])
        assertEquals(2, CategoryManager.categories.size)
    }
}
