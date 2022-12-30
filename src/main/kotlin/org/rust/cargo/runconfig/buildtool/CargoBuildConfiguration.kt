/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import org.rust.cargo.runconfig.CargoAwareConfiguration
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildConfiguration

@Suppress("UnstableApiUsage")
open class CargoBuildConfiguration(
    // val configuration: CargoCommandConfiguration, // TODO use a more abstract ting so i can put
    val configuration: CargoAwareConfiguration,
    val environment: ExecutionEnvironment
) : ProjectModelBuildableElement {
    // open val enabled: Boolean get() = configuration.isBuildToolWindowAvailable // TODO: check it actually works as i expect
    open val enabled: Boolean get() = true // TODO: um? needs to be reverted i guess

    init {
        require(isBuildConfiguration(configuration))
    }

    override fun getExternalSource(): ProjectModelExternalSource? = null
}
