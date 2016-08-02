package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Dennis.Ushakov
 */
interface InstructionVerifier : Opcodes {
  fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext)
}

//TODO: https://examples.javacodegeeks.com/java-basics/exceptions/java-lang-verifyerror-how-to-solve-verifyerror/
//implement the "A wrong argument is passed to a method" use case