/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.*

fun Location.formatUsageLocation() = when (this) {
  is ClassLocation -> formatClassLocation(
    ClassOption.FULL_NAME,
    ClassGenericsSignatureOption.NO_GENERICS
  )
  is MethodLocation -> formatMethodLocation(
    HostClassOption.FULL_HOST_NAME,
    MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME,
    MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME,
    MethodParameterNameOption.NO_PARAMETER_NAMES
  )
  is FieldLocation -> formatFieldLocation(
    HostClassOption.FULL_HOST_NAME,
    FieldTypeOption.SIMPLE_TYPE
  )
}