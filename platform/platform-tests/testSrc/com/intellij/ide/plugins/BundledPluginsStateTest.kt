// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.plugins

import com.intellij.core.CoreBundle
import com.intellij.ide.plugins.BundledPluginsState.Companion.savedBuildNumber
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.assertions.Assertions.assertThat
import java.nio.file.Path

class BundledPluginsStateTest : LightPlatformTestCase() {
  fun testSaving() {
    val pluginIds = listOf(
      "foo" to null,
      "bar" to "UI",
    ).mapTo(LinkedHashSet()) {
      getIdeaDescriptor(it.first, it.second)
    }

    BundledPluginsState.writePluginIdsToFile(pluginIds)
    assertThat(BundledPluginsState.readPluginIdsFromFile())
      .hasSameElementsAs(pluginIds.map { it.pluginId to it.category })
  }

  fun testSavingState() {
    assertThat(BundledPluginsState.readPluginIdsFromFile().map(Pair<PluginId, Category>::first))
      .hasSameElementsAs(BundledPluginsState.loadedPlugins.map(IdeaPluginDescriptor::getPluginId))

    val savedBuildNumber = PropertiesComponent.getInstance().savedBuildNumber
    assertThat(savedBuildNumber).isNotNull
    assertThat(savedBuildNumber).isGreaterThanOrEqualTo(ApplicationInfo.getInstance().build)
  }

  fun testCategoryLocalized() {
    //BundledPluginsState.loadedPlugins.asSequence().filter { it.category != null }.map { it.category }.toSet().toList().sortedBy { it }.toList().forEach {
    //  println("plugin.category.${it?.replace(' ', '.')}=${it}")
    //}
    BundledPluginsState.loadedPlugins.filter { it.category != null }.forEach {
      assertThat(CoreBundle.messageOrNull("plugin.category.${it.category?.replace(' ', '.')}")).isEqualTo(it.category)
    }
  }

  private fun getIdeaDescriptor(id: String, category: Category): IdeaPluginDescriptorImpl {
    val descriptor = IdeaPluginDescriptorImpl(RawPluginDescriptor(), Path.of(""), true, PluginId.getId(id), null)
    descriptor.category = category
    return descriptor
  }
}