plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // CalendarDependencies lists the repositories of core:data
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.forms)
            implementation(projects.core.productcard)
            implementation(projects.core.resources)
            implementation(projects.core.ui)
            implementation(projects.feature.batteries.api)
            implementation(projects.feature.calendar.api)
            implementation(projects.feature.chores.api)
            implementation(projects.feature.masterdata.api)
            implementation(projects.feature.recipes.api)
            implementation(projects.feature.stock.api)
            implementation(projects.feature.tasks.api)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.qrose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
