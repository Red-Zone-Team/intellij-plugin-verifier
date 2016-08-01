package com.jetbrains.pluginverifier.utils;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.function.Predicate;


/**
 * @author Sergey Patrikeev
 */
class ParentsVisitor {

  private final Map<String, Set<String>> myParentsCache = new HashMap<>();

  private final Resolver myResolver;
  private final VContext myCtx;

  ParentsVisitor(@NotNull Resolver resolver, VContext ctx) {
    myResolver = resolver;
    myCtx = ctx;
  }

  /**
   * Iterates through the inheritance tree of the specified class and collects
   * all the unresolved parent-classes
   *
   * @param className         start class
   * @param excludedPredicate whether to treat given class as excluded (and not process it at all)
   * @return set of unresolved classes
   * @throws IllegalArgumentException in case starting class is not found in the {@link #myResolver resolver}
   */
  @NotNull
  Set<String> collectUnresolvedParents(@NotNull String className,
                                       @NotNull Predicate<String> excludedPredicate) throws IllegalArgumentException {
    ClassNode classNode = VerifierUtil.findClass(myResolver, className, myCtx);
    if (classNode == null) {
      throw new IllegalArgumentException(className + " should be found in the resolver " + myResolver);
    }

    if (myParentsCache.containsKey(className)) {
      return myParentsCache.get(className);
    }

    Set<String> allParents = new HashSet<>();

    String superName = classNode.superName;
    if (superName != null) {
      if (!excludedPredicate.test(superName)) {
        if (VerifierUtil.findClass(myResolver, superName, myCtx) != null) {
          allParents.addAll(collectUnresolvedParents(superName, excludedPredicate));
        } else {
          allParents.add(superName);
        }
      }
    }

    @SuppressWarnings("unchecked")
    List<String> interfaces = classNode.interfaces;
    if (interfaces != null) {
      for (String anInterface : interfaces) {
        if (!excludedPredicate.test(anInterface)) {
          if (VerifierUtil.findClass(myResolver, anInterface, myCtx) != null) {
            allParents.addAll(collectUnresolvedParents(anInterface, excludedPredicate));
          } else {
            allParents.add(anInterface);
          }
        }
      }
    }

    myParentsCache.put(className, allParents);
    return allParents;
  }

}