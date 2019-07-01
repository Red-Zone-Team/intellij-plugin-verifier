package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.isSubclassOf

fun isClassAccessibleToOtherClass(me: ClassFile, other: ClassFile): Boolean =
    me.isPublic
        || me.isPrivate && me.name == other.name
        || me.javaPackageName == other.javaPackageName
        || isKotlinDefaultConstructorMarker(me)

/**
 * In Kotlin classes the default constructor has a special parameter of type `DefaultConstructorMarker`.
 * This class is package-private but is never instantiated because `null` is always passed as its value.
 * We should not report "illegal access" for this class.
 */
private fun isKotlinDefaultConstructorMarker(classFile: ClassFile): Boolean =
    classFile.name == "kotlin/jvm/internal/DefaultConstructorMarker"

fun detectAccessProblem(callee: ClassFileMember, caller: ClassFileMember, context: VerificationContext): AccessType? {
  when {
    callee.isPrivate ->
      if (caller.containingClassFile.name != callee.containingClassFile.name) {
        return AccessType.PRIVATE
      }
    callee.isProtected ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        if (!context.classResolver.isSubclassOf(caller.containingClassFile, callee.containingClassFile.name)) {
          return AccessType.PROTECTED
        }
      }
    callee.isPackagePrivate ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        return AccessType.PACKAGE_PRIVATE
      }
  }
  return null
}