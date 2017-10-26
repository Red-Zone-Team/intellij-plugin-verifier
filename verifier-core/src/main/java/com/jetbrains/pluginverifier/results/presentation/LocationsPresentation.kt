package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.convertFieldSignature
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes

/**
 * Converts class name in binary form into Java-like presentation.
 * E.g. 'org/some/Class$Inner1$Inner2' -> 'org.some.Class.Inner1.Inner2'
 */
val toFullJavaClassName: (String) -> String = { binaryName -> binaryName.replace('/', '.').replace('$', '.') }

/**
 * Cuts off the package of the class and converts the simple name of the class to Java-like presentation
 * E.g. 'org/some/Class$Inner1$Inner2' -> 'Class.Inner1.Inner2'
 */
val toSimpleJavaClassName: (String) -> String = { binaryName -> binaryName.substringAfterLast("/").replace('$', '.') }

private fun FieldLocation.toFieldType(fieldTypeOption: FieldTypeOption): String {
  val descriptorConverter = when (fieldTypeOption) {
    FieldTypeOption.NO_HOST -> return ""
    FieldTypeOption.SIMPLE_HOST_NAME -> toSimpleJavaClassName
    FieldTypeOption.FULL_HOST_NAME -> toFullJavaClassName
  }
  return if (signature.isNotEmpty()) {
    convertFieldSignature(signature, descriptorConverter)
  } else {
    convertJvmDescriptorToNormalPresentation(fieldDescriptor, descriptorConverter)
  }
}

fun ClassLocation.formatClassLocation(classLocationOption: ClassOption,
                                      classTypeSignatureOption: ClassGenericsSignatureOption): String {
  val converter = when (classLocationOption) {
    ClassOption.SIMPLE_NAME -> toSimpleJavaClassName
    ClassOption.FULL_NAME -> toFullJavaClassName
  }
  return if (signature.isNotEmpty()) {
    when (classTypeSignatureOption) {
      ClassGenericsSignatureOption.NO_GENERICS -> converter(className)
      ClassGenericsSignatureOption.WITH_GENERICS -> converter(className) + JvmDescriptorsPresentation.convertClassSignature(signature, toSimpleJavaClassName)
    }
  } else {
    converter(className)
  }
}

private fun ClassLocation.formatHostClass(hostClassOption: HostClassOption): String = when (hostClassOption) {
  HostClassOption.NO_HOST -> ""
  HostClassOption.SIMPLE_HOST_NAME -> formatClassLocation(ClassOption.SIMPLE_NAME, ClassGenericsSignatureOption.NO_GENERICS)
  HostClassOption.FULL_HOST_NAME -> formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)
  HostClassOption.FULL_HOST_WITH_SIGNATURE -> formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)
}

fun MethodLocation.formatMethodLocation(hostClassOption: HostClassOption,
                                        methodParameterTypeOption: MethodParameterTypeOption,
                                        methodReturnTypeOption: MethodReturnTypeOption): String = buildString {
  val formattedHost = hostClass.formatHostClass(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append(formattedHost + ".")
  }
  append("$methodName(")
  val (params, returnType) = methodParametersWithNamesAndReturnType(methodParameterTypeOption, methodReturnTypeOption)
  append(params.joinToString())
  append(") : $returnType")
}

fun FieldLocation.formatFieldLocation(hostClassOption: HostClassOption, fieldTypeOption: FieldTypeOption): String = buildString {
  val formattedHost = hostClass.formatHostClass(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append(formattedHost + ".")
  }
  append(fieldName)
  val type = toFieldType(fieldTypeOption)
  if (type.isNotEmpty()) {
    append(" : $type")
  }
}

private fun MethodLocation.zipWithNames(parametersTypes: List<String>): List<String> {
  val names: List<String> = if (parameterNames.size == parametersTypes.size) {
    parameterNames
  } else {
    (0 until parametersTypes.size).map { "arg$it" }
  }
  return parametersTypes.zip(names).map { "${it.first} ${it.second}" }
}

private fun MethodLocation.methodParametersWithNamesAndReturnType(methodParameterTypeOption: MethodParameterTypeOption,
                                                                  methodReturnTypeOption: MethodReturnTypeOption): Pair<List<String>, String> {
  val paramsConverter = when (methodParameterTypeOption) {
    MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME -> toSimpleJavaClassName
    MethodParameterTypeOption.FULL_PARAM_CLASS_NAME -> toFullJavaClassName
  }
  val returnConverter = when (methodReturnTypeOption) {
    MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME -> toSimpleJavaClassName
    MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME -> toFullJavaClassName
  }
  val (parametersTypes, returnType) = if (signature.isNotEmpty()) {
    JvmDescriptorsPresentation.parseMethodSignature(signature, paramsConverter)
  } else {
    val (paramsTs, returnT) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    (paramsTs.map { convertJvmDescriptorToNormalPresentation(it, paramsConverter) }) to (convertJvmDescriptorToNormalPresentation(returnT, returnConverter))
  }
  val withNames = zipWithNames(parametersTypes)
  return withNames to returnType
}