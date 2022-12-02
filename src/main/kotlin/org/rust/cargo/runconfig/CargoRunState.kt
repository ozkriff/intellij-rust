/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig

import com.intellij.execution.runners.ExecutionEnvironment
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.console.CargoConsoleBuilder

class CargoRunState(
    environment: ExecutionEnvironment,
    runConfiguration: CargoAwareConfiguration,
    config: CargoAwareConfiguration.CleanConfiguration.Ok
) : CargoRunStateBase(environment, runConfiguration, config) {
    init {
        consoleBuilder = CargoConsoleBuilder(project, runConfiguration)
        createFilters(cargoProject).forEach { consoleBuilder.addFilter(it) }
    }
}
