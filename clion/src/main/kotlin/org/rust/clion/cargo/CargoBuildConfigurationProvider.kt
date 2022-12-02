/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.clion.cargo

import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.build.CidrBuildConfigurationProvider
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.createBuildEnvironment
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.getBuildConfiguration
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

class CargoBuildConfigurationProvider : CidrBuildConfigurationProvider {
    override fun getBuildableConfigurations(project: Project): List<CLionCargoBuildConfiguration> {
        val runManager = RunManager.getInstance(project) as? RunManagerImpl ?: return emptyList()
        val configuration = runManager.selectedConfiguration?.configuration as? CargoCommandConfiguration
            ?: return emptyList()
        val buildConfiguration = getBuildConfiguration(configuration) ?: return emptyList()
        val environment = createBuildEnvironment(buildConfiguration) ?: return emptyList()

        if (buildConfiguration !is CargoCommandConfiguration) { // TODO: will it work at all?
            assert(false)
            return emptyList()
        }

        return listOf(CLionCargoBuildConfiguration(buildConfiguration, environment))
    }
}
