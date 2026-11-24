package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.recipes.InstructionSection
import com.patrickzedler.grocy.core.model.recipes.RecipeNutrient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipePageTest {

    @Test
    fun readsEverythingARecipeSiteTellsSearchEngines() {
        val url = "https://www.chefkoch.de/rezepte/1/x.html"

        val recipe = parseRecipePage(page(CHEFKOCH_STYLE), url)!!

        assertEquals("Apfelkuchen mit Streuseln", recipe.name)
        assertEquals("Saftig & einfach", recipe.description)
        assertEquals(12, recipe.servings)
        assertEquals(listOf("300 g Mehl", "1 Ei(er)"), recipe.ingredients)
        assertEquals(
            listOf(InstructionSection("Zubereitung", listOf("Teig kneten.", "Backen."))),
            recipe.instructions,
        )
        assertEquals("https://img.chefkoch-cdn.de/apfelkuchen.jpg", recipe.imageUrl)
        assertEquals(30, recipe.prepMinutes)
        assertEquals(45, recipe.cookMinutes)
        assertEquals(75, recipe.totalMinutes)
        assertEquals(
            mapOf(RecipeNutrient.CALORIES to "320 kcal", RecipeNutrient.PROTEIN to "4 g"),
            recipe.nutrition,
        )
        assertEquals("Backen, Kuchen", recipe.category)
        assertEquals("Deutschland", recipe.cuisine)
        assertEquals("heresbach", recipe.author)
        assertEquals("https://www.chefkoch.de/rezepte/1/x.html", recipe.sourceUrl)
    }

    @Test
    fun findsTheRecipeInAGraphBetweenOtherObjects() {
        val json = """
            {"@context": "https://schema.org", "@graph": [
              {"@type": "WebSite", "name": "Good Food"},
              {"@type": ["Recipe", "NewsArticle"], "name": "Carbonara",
               "recipeYield": ["4", "4 servings"],
               "recipeInstructions": [{"@type": "HowToStep", "text": "Boil.", "name": "Boil."},
                                      {"@type": "HowToStep", "text": "Mix."}],
               "image": [{"@type": "ImageObject", "url": "/images/carbonara.jpg"}]}
            ]}
        """.trimIndent()

        val recipe = parseRecipePage(page(json), "https://www.example.com/recipes/carbonara")!!

        assertEquals("Carbonara", recipe.name)
        assertEquals(4, recipe.servings)
        assertEquals(listOf(InstructionSection(null, listOf("Boil.", "Mix."))), recipe.instructions)
        assertEquals("https://www.example.com/images/carbonara.jpg", recipe.imageUrl)
    }

    @Test
    fun valuesPointingToOtherObjectsOfThePageAreFollowed() {
        val json = """
            {"@graph": [
              {"@type": "Recipe", "name": "Kuchen",
               "image": {"@id": "https://x.de/r#image"}, "author": {"@id": "https://x.de/u#me"}},
              {"@type": "ImageObject", "@id": "https://x.de/r#image",
               "url": "https://img.x.de/kuchen.jpg"},
              {"@type": "Person", "@id": "https://x.de/u#me", "name": "heresbach"}
            ]}
        """.trimIndent()

        val recipe = parseRecipePage(page(json), "https://x.de/r")!!

        assertEquals("https://img.x.de/kuchen.jpg", recipe.imageUrl)
        assertEquals("heresbach", recipe.author)
    }

    @Test
    fun withoutPictureInTheRecipeTheSharedPictureOfThePageIsTaken() {
        val html = """<meta property="og:image" content="https://img.x.de/a.jpg">""" +
            page("""{"@type": "Recipe", "name": "Brot"}""")

        assertEquals("https://img.x.de/a.jpg", parseRecipePage(html, "https://x.de/b")?.imageUrl)
    }

    @Test
    fun nutritionValuesGetTheirShortUnit() {
        assertEquals("30.03 g", nutritionValue("30.03 grams fat"))
        assertEquals("656 kcal", nutritionValue("656 calories"))
        assertEquals("1.65 mg", nutritionValue("1.65 milligram of sodium"))
        assertEquals("320 kcal", nutritionValue("320 kcal"))
        assertEquals("ca. 5 g", nutritionValue("ca. 5 g"))
    }

    @Test
    fun aPageWithSeveralScriptsUsesTheOneWithTheRecipe() {
        val html = page("""{"@type": "Organization", "name": "Site"}""") +
            page("""[{"@type": "BreadcrumbList"}, {"@type": "Recipe", "name": "Soup"}]""")

        assertEquals("Soup", parseRecipePage(html, "https://example.com/soup")?.name)
    }

    @Test
    fun instructionsAsOneTextBecomeOneStepPerLine() {
        val json = """{"@type": "Recipe", "name": "Tea",
            "recipeInstructions": "Boil water.<br>Pour it.\n\nWait."}"""

        val recipe = parseRecipePage(page(json), "https://example.com/tea")!!

        assertEquals(
            listOf(InstructionSection(null, listOf("Boil water.", "Pour it.", "Wait."))),
            recipe.instructions,
        )
    }

    @Test
    fun entitiesAndTagsBecomePlainText() {
        val json = """{"@type": "Recipe", "name": "P&acirc;tes &quot;carbonara&quot;",
            "description": "<p>Tr&egrave;s <b>bon</b>&nbsp;!</p>"}"""

        val recipe = parseRecipePage(page(json), "https://example.com/pates")!!

        assertEquals("P&acirc;tes \"carbonara\"", recipe.name)
        assertEquals("Très bon !", recipe.description)
    }

    @Test
    fun rawLineBreaksInStringsDoNotHideTheRecipe() {
        val json = "{\"@type\": \"Recipe\", \"name\": \"Line\nbreak\"}"

        assertEquals("Line break", parseRecipePage(page(json), "https://example.com/x")?.name)
    }

    @Test
    fun aPageWithoutRecipeHasNone() {
        assertNull(parseRecipePage(page("""{"@type": "Article", "name": "News"}"""), "https://x.y"))
        assertNull(parseRecipePage("<html><body>No data</body></html>", "https://x.y"))
    }

    @Test
    fun durationsAreMinutes() {
        assertEquals(75, minutesOf("PT1H15M"))
        assertEquals(20, minutesOf("P0DT0H20M"))
        assertEquals(1500, minutesOf("P1DT1H"))
        assertEquals(30, minutesOf("PT0.5H"))
        assertNull(minutesOf("PT0M"))
        assertNull(minutesOf("20 minutes"))
    }

    @Test
    fun onlyWebAddressesAreRequested() {
        assertEquals(true, isWebAddress(" https://www.chefkoch.de/rezepte/1.html "))
        assertEquals(true, isWebAddress("http://example.com"))
        assertEquals(false, isWebAddress("file:///etc/passwd"))
        assertEquals(false, isWebAddress("www.chefkoch.de"))
        assertEquals(false, isWebAddress("https://"))
        assertEquals(false, isWebAddress("https://exa mple.com"))
    }

    private fun page(json: String) =
        """<html><head><script type="application/ld+json">$json</script></head></html>"""

    private companion object {
        val CHEFKOCH_STYLE = """
            {"@context": "http://schema.org", "@type": "Recipe",
             "name": "Apfelkuchen mit Streuseln von heresbach",
             "description": "Saftig &amp; einfach",
             "recipeYield": "12 Portionen", "prepTime": "PT30M", "cookTime": "PT45M",
             "totalTime": "PT1H15M",
             "recipeIngredient": ["300 g Mehl", "1 Ei(er)"],
             "recipeInstructions": [{"@type": "HowToSection", "name": "Zubereitung",
               "itemListElement": [{"@type": "HowToStep", "text": "Teig kneten."},
                                   {"@type": "HowToStep", "text": "Backen."}]}],
             "image": {"@type": "ImageObject",
               "url": "https://img.chefkoch-cdn.de/apfelkuchen.jpg"},
             "nutrition": {"@type": "NutritionInformation", "calories": "320 kcal",
               "proteinContent": "4 g"},
             "recipeCategory": ["Backen", "Kuchen"], "recipeCuisine": "Deutschland",
             "author": {"@type": "Person", "name": "heresbach"}}
        """.trimIndent()
    }
}
