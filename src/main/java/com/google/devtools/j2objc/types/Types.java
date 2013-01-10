/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.util.NameTable;
import com.google.inject.Singleton;
import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Types is a singleton service class for type-related operations.
 *
 * @author Tom Ball
 */
@Singleton
public class Types implements TypeService {
  private AST ast;
  private Map<Object, IBinding> bindingMap;
  private Map<ITypeBinding, ITypeBinding> typeMap;
  private Map<ITypeBinding, ITypeBinding> renamedTypeMap;
  private Map<String, String> simpleTypeMap;
  private Map<IMethodBinding, IOSMethod> mappedMethods;
  private Map<Expression, IMethodBinding> mappedInvocations;
  private Map<IVariableBinding, IVariableBinding> mappedVariables;
  private Map<ASTNode, ASTNode> substitutionMap;
  private Map<IVariableBinding, ITypeBinding> variablesNeedingCasts;
  private List<IMethodBinding> functions;
  private Map<ITypeBinding, ITypeBinding> primitiveToWrapperTypes;
  private Map<ITypeBinding, ITypeBinding> wrapperToPrimitiveTypes;
  private List<IVariableBinding> releaseableFields;
  private ITypeBinding javaObjectType;
  private ITypeBinding javaClassType;
  private ITypeBinding javaCloneableType;
  private ITypeBinding javaNumberType;
  private ITypeBinding javaStringType;
  private ITypeBinding javaVoidType;
  private ITypeBinding voidType;
  private ITypeBinding booleanType;

  // Non-standard naming pattern is used, since in this case it's more readable.
  public final IOSTypeBinding NSCopying = new IOSTypeBinding("NSCopying", true);
  public final IOSTypeBinding NSObject = new IOSTypeBinding("NSObject", false);
  public final IOSTypeBinding NSNumber = new IOSTypeBinding("NSNumber", NSObject);
  public final IOSTypeBinding NSString = new IOSTypeBinding("NSString", NSObject);
  public final IOSTypeBinding JavaLangCharSequence =
      new IOSTypeBinding("JavaLangCharSequence", true);
  public final IOSTypeBinding NS_ANY = new IOSTypeBinding("id", false);
  public final IOSTypeBinding IOSClass = new IOSTypeBinding("IOSClass", false);

  public IOSArrayTypeBinding IOSBooleanArray;
  public IOSArrayTypeBinding IOSByteArray;
  public IOSArrayTypeBinding IOSCharArray;
  public IOSArrayTypeBinding IOSDoubleArray;
  public IOSArrayTypeBinding IOSFloatArray;
  public IOSArrayTypeBinding IOSIntArray;
  public IOSArrayTypeBinding IOSLongArray;
  public IOSArrayTypeBinding IOSObjectArray;
  public IOSArrayTypeBinding IOSShortArray;

  private Map<String, IOSTypeBinding> iosBindingMap;

  private Map<ITypeBinding, String> primitiveTypeNameMap;

  // Map a primitive type to its emulation array type.
  private Map<String, IOSArrayTypeBinding> arrayTypeMap;
  private Map<ITypeBinding, IOSArrayTypeBinding> arrayBindingMap;
  private Map<IOSArrayTypeBinding, ITypeBinding> componentTypeMap;

  private static final int STATIC_FINAL_MODIFIERS = Modifier.STATIC | Modifier.FINAL;

  @Override
  public void initialize(CompilationUnit unit) {
    ast = unit.getAST();

    typeMap = Maps.newHashMap();
    renamedTypeMap = Maps.newHashMap();
    simpleTypeMap = Maps.newHashMap();
    mappedMethods = Maps.newHashMap();
    mappedInvocations = Maps.newHashMap();
    mappedVariables = Maps.newHashMap();
    substitutionMap = Maps.newHashMap();
    variablesNeedingCasts = Maps.newHashMap();
    functions = Lists.newArrayList();
    primitiveToWrapperTypes = new HashMap<ITypeBinding, ITypeBinding>();
    wrapperToPrimitiveTypes = new HashMap<ITypeBinding, ITypeBinding>();
    releaseableFields = Lists.newArrayList();

    iosBindingMap = Maps.newHashMap();
    primitiveTypeNameMap = Maps.newHashMap();
    arrayTypeMap = Maps.newHashMap();
    arrayBindingMap = Maps.newHashMap();
    componentTypeMap = Maps.newHashMap();

    initializeBaseClasses();
    javaObjectType = ast.resolveWellKnownType("java.lang.Object");
    javaClassType = ast.resolveWellKnownType("java.lang.Class");
    javaCloneableType = ast.resolveWellKnownType("java.lang.Cloneable");
    javaStringType = ast.resolveWellKnownType("java.lang.String");
    javaVoidType = ast.resolveWellKnownType("java.lang.Void");
    voidType = ast.resolveWellKnownType("void");
    booleanType = ast.resolveWellKnownType("boolean");
    NSObject.setMappedType(javaObjectType);
    NSString.setMappedType(javaStringType);
    ITypeBinding binding = ast.resolveWellKnownType("java.lang.Integer");
    javaNumberType = binding.getSuperclass();
    initializeArrayTypes();
    initializeTypeMap();
    populateSimpleTypeMap();
    populateArrayTypeMaps();
    populatePrimitiveTypeNameMap();
    populatePrimitiveAndWrapperTypeMaps();
    bindingMap = BindingMapBuilder.buildBindingMap(unit);
    setGlobalRenamings();
  }

  private void initializeBaseClasses() {
    iosBindingMap.put("NSObject", NSObject);
    iosBindingMap.put("IOSClass", IOSClass);
    iosBindingMap.put("NSString", NSString);
    iosBindingMap.put("NSNumber", NSNumber);
    iosBindingMap.put("NSCopying", NSCopying);
    iosBindingMap.put("id", NS_ANY);
  }

  private void initializeArrayTypes() {
    IOSBooleanArray = new IOSArrayTypeBinding(
        "IOSBooleanArray", "arrayWithBooleans", "booleanAtIndex", "getBooleans",
        ast.resolveWellKnownType("java.lang.Boolean"), ast.resolveWellKnownType("boolean"));
    IOSByteArray =
        new IOSArrayTypeBinding("IOSByteArray", "arrayWithBytes", "byteAtIndex", "getBytes",
            ast.resolveWellKnownType("java.lang.Byte"), ast.resolveWellKnownType("byte"));
    IOSCharArray =
        new IOSArrayTypeBinding("IOSCharArray", "arrayWithCharacters", "charAtIndex", "getChars",
            ast.resolveWellKnownType("java.lang.Character"), ast.resolveWellKnownType("char"));
    IOSDoubleArray =
        new IOSArrayTypeBinding("IOSDoubleArray", "arrayWithDoubles", "doubleAtIndex", "getDoubles",
            ast.resolveWellKnownType("java.lang.Double"), ast.resolveWellKnownType("double"));
    IOSFloatArray =
        new IOSArrayTypeBinding("IOSFloatArray", "arrayWithFloats", "floatAtIndex", "getFloats",
            ast.resolveWellKnownType("java.lang.Float"), ast.resolveWellKnownType("float"));
    IOSIntArray =
        new IOSArrayTypeBinding("IOSIntArray", "arrayWithInts", "intAtIndex", "getInts",
            ast.resolveWellKnownType("java.lang.Integer"), ast.resolveWellKnownType("int"));
    IOSLongArray =
        new IOSArrayTypeBinding("IOSLongArray", "arrayWithLongs", "longAtIndex", "getLongs",
            ast.resolveWellKnownType("java.lang.Long"), ast.resolveWellKnownType("long"));
    IOSObjectArray =
        new IOSArrayTypeBinding("IOSObjectArray", "arrayWithObjects", "objectAtIndex", "getObjects",
            ast.resolveWellKnownType("java.lang.Object"), null);
    IOSShortArray =
        new IOSArrayTypeBinding("IOSShortArray", "arrayWithShorts", "shortAtIndex", "getShorts",
            ast.resolveWellKnownType("java.lang.Short"), ast.resolveWellKnownType("short"));

    iosBindingMap.put("IOSBooleanArray", IOSBooleanArray);
    iosBindingMap.put("IOSByteArray", IOSByteArray);
    iosBindingMap.put("IOSCharArray", IOSCharArray);
    iosBindingMap.put("IOSDoubleArray", IOSDoubleArray);
    iosBindingMap.put("IOSFloatArray", IOSFloatArray);
    iosBindingMap.put("IOSIntArray", IOSIntArray);
    iosBindingMap.put("IOSLongArray", IOSLongArray);
    iosBindingMap.put("IOSObjectArray", IOSObjectArray);
    iosBindingMap.put("IOSShortArray", IOSShortArray);
    iosBindingMap.put("JavaLangCharSequence", JavaLangCharSequence);
  }

  /**
   * Initialize type map with classes that are explicitly mapped to an iOS
   * type.
   *
   * NOTE: if this method's list is changed, IOSClass.forName() needs to be
   * similarly updated.
   */
  private void initializeTypeMap() {
    typeMap.put(javaObjectType, NSObject);
    typeMap.put(javaClassType, IOSClass);
    typeMap.put(javaCloneableType, NSCopying);
    typeMap.put(javaStringType, NSString);

    // Number isn't a well-known type, but its subclasses are.
    typeMap.put(javaNumberType, NSNumber);
    NSNumber.setMappedType(javaNumberType.getSuperclass());
  }

  private void populateSimpleTypeMap() {
    simpleTypeMap.put("JavaLangObject", "NSObject");
    simpleTypeMap.put("JavaLangString", "NSString");
    simpleTypeMap.put("JavaLangNumber", "NSNumber");
    simpleTypeMap.put("JavaLangCloneable", "NSCopying");
  }

  private void populateArrayTypeMaps() {
    arrayTypeMap.put("boolean", IOSBooleanArray);
    arrayTypeMap.put("byte", IOSByteArray);
    arrayTypeMap.put("char", IOSCharArray);
    arrayTypeMap.put("double", IOSDoubleArray);
    arrayTypeMap.put("float", IOSFloatArray);
    arrayTypeMap.put("int", IOSIntArray);
    arrayTypeMap.put("long", IOSLongArray);
    arrayTypeMap.put("short", IOSShortArray);
    addPrimitiveMappings("boolean", IOSBooleanArray);
    addPrimitiveMappings("byte", IOSByteArray);
    addPrimitiveMappings("char", IOSCharArray);
    addPrimitiveMappings("double", IOSDoubleArray);
    addPrimitiveMappings("float", IOSFloatArray);
    addPrimitiveMappings("int", IOSIntArray);
    addPrimitiveMappings("long", IOSLongArray);
    addPrimitiveMappings("short", IOSShortArray);
  }

  private void addPrimitiveMappings(String typeName, IOSArrayTypeBinding arrayType) {
    ITypeBinding primitiveType = ast.resolveWellKnownType(typeName);
    arrayBindingMap.put(primitiveType, arrayType);
    componentTypeMap.put(arrayType, primitiveType);
  }

  private void populatePrimitiveTypeNameMap() {
    primitiveTypeNameMap.put(ast.resolveWellKnownType("boolean"), "BOOL");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("byte"), "char");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("char"), "unichar");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("double"), "double");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("float"), "float");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("int"), "int");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("long"), "long long");
    primitiveTypeNameMap.put(ast.resolveWellKnownType("short"), "short");
  }

  private void populatePrimitiveAndWrapperTypeMaps() {
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("boolean"), ast.resolveWellKnownType("java.lang.Boolean"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("byte"), ast.resolveWellKnownType("java.lang.Byte"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("char"), ast.resolveWellKnownType("java.lang.Character"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("short"), ast.resolveWellKnownType("java.lang.Short"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("int"), ast.resolveWellKnownType("java.lang.Integer"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("long"), ast.resolveWellKnownType("java.lang.Long"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("float"), ast.resolveWellKnownType("java.lang.Float"));
    loadPrimitiveAndWrapperTypes(
        ast.resolveWellKnownType("double"), ast.resolveWellKnownType("java.lang.Double"));
  }

  private void loadPrimitiveAndWrapperTypes(ITypeBinding primitive, ITypeBinding wrapper) {
    primitiveToWrapperTypes.put(primitive, wrapper);
    wrapperToPrimitiveTypes.put(wrapper, primitive);
  }

  private void setGlobalRenamings() {
    // longValue => longLongValue, because of return value
    // difference with NSNumber.longValue.
    renameLongValue(ast.resolveWellKnownType("java.lang.Byte"));
    renameLongValue(ast.resolveWellKnownType("java.lang.Double"));
    renameLongValue(ast.resolveWellKnownType("java.lang.Float"));
    renameLongValue(ast.resolveWellKnownType("java.lang.Integer"));
    renameLongValue(ast.resolveWellKnownType("java.lang.Long"));
    renameLongValue(ast.resolveWellKnownType("java.lang.Short"));
  }

  void renameLongValue(ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (method.getName().equals("longValue")) {
        NameTable.rename(method, "longLongValue");
        break;
      }
    }
  }

  /**
   * If this method overrides another method, return the binding for the
   * original declaration.
   */
  @Override
  public IMethodBinding getOriginalMethodBinding(IMethodBinding method) {
    if (method != null) {
      ITypeBinding clazz = method.getDeclaringClass();
      ITypeBinding superclass = clazz.getSuperclass();
      if (superclass != null) {
        for (IMethodBinding interfaceMethod : superclass.getDeclaredMethods()) {
          if (!(interfaceMethod instanceof IOSMethodBinding) && method.overrides(interfaceMethod)) {
            IMethodBinding decl = interfaceMethod.getMethodDeclaration();
            return decl != null ? decl : interfaceMethod.getMethodDeclaration();
          }
        }
      }

      // Collect all interfaces implemented by this class.
      Set<ITypeBinding> allInterfaces = Sets.newHashSet();
      while (clazz != null) {
        allInterfaces.addAll(getAllInterfaces(clazz));
        clazz = clazz.getSuperclass();
      }

      for (ITypeBinding interfaceBinding : allInterfaces) {
        for (IMethodBinding interfaceMethod : interfaceBinding.getDeclaredMethods()) {
          if (method.overrides(interfaceMethod)) {
            IMethodBinding decl = interfaceMethod.getMethodDeclaration();
            return decl != null ? decl : interfaceMethod.getMethodDeclaration();
          }
        }
      }

    }
    return method;
  }

  /**
   * Returns all interfaces implemented by the given class, and all
   * super-interfaces of those.
   */
  private static Set<ITypeBinding> getAllInterfaces(ITypeBinding type) {
    Set<ITypeBinding> allInterfaces = Sets.newHashSet();
    Deque<ITypeBinding> interfaceQueue = Lists.newLinkedList();

    interfaceQueue.addAll(Arrays.asList(type.getInterfaces()));
    while (!interfaceQueue.isEmpty()) {
      ITypeBinding intrface = interfaceQueue.poll();
      allInterfaces.add(intrface);
      interfaceQueue.addAll(Arrays.asList(intrface.getInterfaces()));
    }

    return allInterfaces;
  }
  /**
   * Returns true if the specified binding is for a static final variable.
   */
  @Override
  public boolean isConstantVariable(IVariableBinding binding) {
    return (binding.getModifiers() & Types.STATIC_FINAL_MODIFIERS) == Types.STATIC_FINAL_MODIFIERS;
  }

  @Override
  public boolean isStaticVariable(IVariableBinding binding) {
    return (binding.getModifiers() & Modifier.STATIC) > 0;
  }

  @Override
  public boolean isPrimitiveConstant(IVariableBinding binding) {
    return binding != null && isConstantVariable(binding) && binding.getType().isPrimitive() &&
        binding.getConstantValue() != null;
  }

  /**
   * Given a JDT type binding created by the parser, either replace it with an iOS
   * equivalent, or return the given type.
   */
  @Override
  public ITypeBinding mapType(ITypeBinding binding) {
    if (binding == null) {  // happens when mapping a primitive type
      return null;
    }
    if (binding.isArray()) {
      return resolveArrayType(binding.getComponentType());
    }
    ITypeBinding newBinding = typeMap.get(binding);
    if (newBinding == null && binding.isAssignmentCompatible(javaClassType)) {
      newBinding = typeMap.get(javaClassType);
    }
    return newBinding != null ? newBinding : binding;
  }

  /**
   * Given a fully-qualified type name, return its binding.
   */
  @Override
  public ITypeBinding mapTypeName(String typeName) {
    ITypeBinding binding = ast.resolveWellKnownType(typeName);
    return mapType(binding);
  }

  /**
   * Returns whether a given type has an iOS equivalent.
   */
  @Override
  public boolean hasIOSEquivalent(ITypeBinding binding) {
    return binding.isArray() || typeMap.containsKey(binding.getTypeDeclaration());
  }

  /**
   * Returns true if a Type AST node refers to an iOS type.
   */
  @Override
  public boolean isIOSType(Type type) {
    return isIOSType(type.toString());
  }

  /**
   * Returns true if a type name refers to an iOS type.
   */
  @Override
  public boolean isIOSType(String name) {
    return simpleTypeMap.get(name) != null
        || simpleTypeMap.containsValue(name);
  }

  /**
   * Returns a simple (no package) name for a given one.
   */
  @Override
  public String mapSimpleTypeName(String typeName) {
    String newName = simpleTypeMap.get(typeName);
    return newName != null ? newName : typeName;
  }

  /**
   * Returns a Type AST node for a specific type binding.
   */
  @Override
  public Type makeType(ITypeBinding binding) {
    Type type;
    if (binding.isPrimitive()) {
      PrimitiveType.Code typeCode = PrimitiveType.toCode(binding.getName());
      type = ast.newPrimitiveType(typeCode);
    } else if (binding.isArray() && !(binding instanceof IOSArrayTypeBinding)) {
      Type componentType = makeType(binding.getComponentType());
      type = ast.newArrayType(componentType);
    } else {
      String typeName = binding.getErasure().getName();
      if (typeName == "") {
        // Debugging aid for anonymous (no-name) classes.
        typeName = "$Local$";
      }
      SimpleName name = ast.newSimpleName(typeName);
      addBinding(name, binding);
      type = ast.newSimpleType(name);
    }
    addBinding(type, binding);
    return type;
  }

  /**
   * Creates a replacement iOS type for a given JDT type.
   */
  @Override
  public Type makeIOSType(Type type) {
    ITypeBinding binding = getTypeBinding(type);
    return makeIOSType(binding);
  }

  @Override
  public Type makeIOSType(ITypeBinding binding) {
    if (binding.isArray()) {
      ITypeBinding componentType = binding.getComponentType();
      return makeType(resolveArrayType(componentType));
    }
    ITypeBinding newBinding = mapType(binding);
    return binding != newBinding ? makeType(newBinding) : null;
  }

  /**
   * Returns true if a specified method binding refers to a replacement iOS
   * type.
   */
  @Override
  public boolean isMappedMethod(IMethodBinding method) {
    return method instanceof IOSMethodBinding ? true : mappedMethods.containsKey(method);
  }

  @Override
  public void addMappedIOSMethod(IMethodBinding binding, IOSMethod method) {
    mappedMethods.put(binding, method);
    addBinding(method, binding);
  }

  @Override
  public IOSMethod getMappedMethod(IMethodBinding binding) {
    return mappedMethods.get(binding);
  }

  /**
   * Returns true if a specified variable binding refers has a replacement.
   */
  @Override
  public boolean isMappedVariable(IVariableBinding var) {
    return mappedVariables.containsKey(var);
  }

  @Override
  public void addMappedVariable(ASTNode node, IVariableBinding newBinding) {
    IVariableBinding oldBinding = getVariableBinding(node);
    assert oldBinding != null;
    mappedVariables.put(oldBinding, newBinding);
  }

  @Override
  public IVariableBinding getMappedVariable(IVariableBinding binding) {
    IVariableBinding var = mappedVariables.get(binding);
    return var != null ? var : binding;
  }

  @Override
  public void addMappedInvocation(Expression method, IMethodBinding binding) {
    mappedInvocations.put(method, binding);
    addBinding(method, binding);
  }

  @Override
  public IMethodBinding resolveInvocationBinding(Expression invocation) {
    if (mappedInvocations.containsKey(invocation)) {
      return mappedInvocations.get(invocation);
    }
    return null;
  }

  @Override
  public IOSTypeBinding resolveIOSType(String name) {
    return iosBindingMap.get(name);
  }

  @Override
  public boolean isJavaObjectType(ITypeBinding type) {
    return javaObjectType.equals(type);
  }

  @Override
  public boolean isJavaStringType(ITypeBinding type) {
    return javaStringType.equals(type);
  }

  @Override
  public boolean isJavaNumberType(ITypeBinding type) {
    return type.isAssignmentCompatible(javaNumberType);
  }

  @Override
  public boolean isFloatingPointType(ITypeBinding type) {
    return type.isEqualTo(ast.resolveWellKnownType("double")) ||
        type.isEqualTo(ast.resolveWellKnownType("float")) ||
        type == ast.resolveWellKnownType("java.lang.Double") ||
        type == ast.resolveWellKnownType("java.lang.Float");
  }

  @Override
  public boolean isBooleanType(ITypeBinding type) {
    return booleanType.equals(type);
  }

  @Override
  public ITypeBinding resolveIOSType(Type type) {
    if (type instanceof SimpleType) {
      String name = ((SimpleType) type).getName().getFullyQualifiedName();
      return resolveIOSType(name);
    }
    return null;
  }

  @Override
  public IOSTypeBinding resolveArrayType(String name) {
    return arrayTypeMap.get(name);
  }

  @Override
  public IOSArrayTypeBinding resolveArrayType(ITypeBinding binding) {
    IOSArrayTypeBinding arrayBinding = arrayBindingMap.get(binding);
    return arrayBinding != null ? arrayBinding : IOSObjectArray;
  }

  @Override
  public String getPrimitiveTypeName(ITypeBinding binding) {
    return primitiveTypeNameMap.get(binding);
  }

  @Override
  public IBinding getBinding(Object node) {
    IBinding binding = bindingMap.get(node);
    assert binding != null;
    return binding;
  }

  @Override
  public void addBinding(Object node, IBinding binding) {
    assert binding != null;
    bindingMap.put(node, binding);
  }

  /**
   * Return a type binding for a specified ASTNode or IOS node, or null if
   * no type binding exists.
   */
  @Override
  public ITypeBinding getTypeBinding(Object node) {
    IBinding binding = getBinding(node);
    if (binding instanceof ITypeBinding) {
      return (ITypeBinding) binding;
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding m = (IMethodBinding) binding;
      return m.isConstructor() ? m.getDeclaringClass() : m.getReturnType();
    } else if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getType();
    }
    return null;
  }

  @Override
  public IMethodBinding getMethodBinding(Object node) {
    IBinding binding = getBinding(node);
    return binding instanceof IMethodBinding ? ((IMethodBinding) binding) : null;
  }

  @Override
  public IVariableBinding getVariableBinding(Object node) {
    IBinding binding = getBinding(node);
    return binding instanceof IVariableBinding ? ((IVariableBinding) binding) : null;
  }

  /**
   * Walks an AST and asserts there is a resolved binding for every
   * ASTNode type that is supposed to have one.
   */
  @Override
  public void verifyNode(ASTNode node) {
    BindingMapVerifier.verify(node, bindingMap);
  }

  @Override
  public void verifyNodes(List<? extends ASTNode> nodes) {
    for (ASTNode node : nodes) {
      BindingMapVerifier.verify(node, bindingMap);
    }
  }

  @Override
  public void substitute(ASTNode oldNode, ASTNode replacement) {
    substitutionMap.put(oldNode, replacement);
  }

  @Override
  public ASTNode getNode(ASTNode currentNode) {
    return substitutionMap.get(currentNode);
  }

  ITypeBinding getIOSArrayComponentType(IOSArrayTypeBinding arrayType) {
    ITypeBinding type = componentTypeMap.get(arrayType);
    return type != null ? type : NSObject;
  }

  @Override
  public ITypeBinding renameTypeBinding(String newName, ITypeBinding newDeclaringClass,
      ITypeBinding originalBinding) {
    ITypeBinding renamedBinding =
        RenamedTypeBinding.rename(newName, newDeclaringClass, originalBinding);
    renamedTypeMap.put(originalBinding, renamedBinding);
    return renamedBinding;
  }

  @Override
  public ITypeBinding getRenamedBinding(ITypeBinding original) {
    return original != null && renamedTypeMap.containsKey(original)
        ? renamedTypeMap.get(original) : original;
  }

  @Override
  public void addFunction(IMethodBinding binding) {
    functions.add(binding);
  }

  @Override
  public boolean isFunction(IMethodBinding binding) {
    if (functions.contains(binding)) {
      return true;
    }
    IMethodBinding decl = binding.getMethodDeclaration();
    return decl != null ? functions.contains(decl) : false;
  }

  @Override
  public boolean isVoidType(Type type) {
    return isVoidType(getTypeBinding(type));
  }

  @Override
  public boolean isVoidType(ITypeBinding type) {
    return type.isEqualTo(voidType);
  }

  @Override
  public boolean isJavaVoidType(ITypeBinding type) {
    return type.isEqualTo(javaVoidType);
  }

  /**
   * Returns the declaration for a specified binding from a list of
   * type declarations.
   */
  @Override
  public TypeDeclaration getTypeDeclaration(ITypeBinding binding, List<?> declarations) {
    binding = binding.getTypeDeclaration();
    for (Object decl : declarations) {
      ITypeBinding type = getTypeBinding(decl).getTypeDeclaration();
      if (binding.isEqualTo(type)) {
        return decl instanceof TypeDeclaration ? (TypeDeclaration) decl : null;
      }
    }
    return null;
  }

  /**
   * Adds a variable that needs to be cast when referenced.  This is necessary
   * for gcc to verify parameters of generic interface's methods
   */
  @Override
  public void addVariableCast(IVariableBinding var, ITypeBinding castType) {
    variablesNeedingCasts.put(var.getVariableDeclaration(), castType);
  }

  @Override
  public boolean variableHasCast(IVariableBinding var) {
    return variablesNeedingCasts.containsKey(var.getVariableDeclaration());
  }

  @Override
  public ITypeBinding getCastForVariable(IVariableBinding var) {
    return variablesNeedingCasts.get(var.getVariableDeclaration());
  }

  @Override
  public void addReleaseableFields(Collection<IVariableBinding> fields) {
    for (IVariableBinding field : fields) {
      releaseableFields.add(field.getVariableDeclaration());
    }
  }

  @Override
  public boolean isReleaseableField(IVariableBinding var) {
    return var != null ? releaseableFields.contains(var.getVariableDeclaration()) : false;
  }

  @Override
  public NullLiteral newNullLiteral() {
    NullLiteral nullLiteral = ast.newNullLiteral();
    addBinding(nullLiteral, NullType.SINGLETON);
    return nullLiteral;
  }

  @Override
  public SimpleName newLabel(String identifier) {
    SimpleName node = ast.newSimpleName(identifier);
    addBinding(node, new IOSTypeBinding(identifier, false));
    return node;
  }

  @Override
  public boolean isJUnitTest(ITypeBinding type) {
    // Skip JUnit framework classes.
    if (type.getPackage().getName().equals("junit.framework")) {
      return false;
    }
    if (Modifier.isAbstract(type.getModifiers())) {
      return false;
    }
    while (type != null) {
      for (ITypeBinding intrf : type.getInterfaces()) {
        if (intrf.getQualifiedName().equals("junit.framework.Test")) {
          return true;
        }
        if (isJUnitTest(intrf)) { // Also check any super-interfaces.
          return true;
        }
      }
      type = type.getSuperclass();
    }
    return false;
  }

  @Override
  public ITypeBinding getWrapperType(ITypeBinding primitiveType) {
    return primitiveToWrapperTypes.get(primitiveType);
  }

  @Override
  public ITypeBinding getPrimitiveType(ITypeBinding wrapperType) {
    return wrapperToPrimitiveTypes.get(wrapperType);
  }

  @Override
  public ITypeBinding getNSNumber() {
    return NSNumber;
  }

  @Override
  public ITypeBinding getNSObject() {
    return NSObject;
  }

  @Override
  public ITypeBinding getNSString() {
    return NSString;
  }

  @Override
  public ITypeBinding getIOSClass() {
    return IOSClass;
  }

  @Override
  public boolean isWeakReference(IVariableBinding var) {
    if (hasWeakAnnotation(var)) {
      return true;
    }
    return hasWeakAnnotation(var.getType());
  }

  @Override
  public boolean hasAnyAnnotation(IBinding binding, Class<?>[] annotations) {
    for (IAnnotationBinding annotation : binding.getAnnotations()) {
      String name = annotation.getAnnotationType().getQualifiedName();
      for (Class<?> annotationClass : annotations) {
        if (name.equals(annotationClass.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean hasAnnotation(IBinding binding, Class<?> annotation) {
    return hasAnyAnnotation(binding, new Class<?>[] { annotation });
  }

  @Override
  public boolean hasWeakAnnotation(IBinding binding) {
    return hasAnyAnnotation(binding, new Class<?>[] { Weak.class, WeakOuter.class });
  }

  @Override
  public boolean hasAutoreleasePoolAnnotation(IBinding binding) {
    boolean hasAnnotation = hasAnnotation(binding, AutoreleasePool.class);

    if (hasAnnotation && binding instanceof IMethodBinding) {
      if (!isVoidType(((IMethodBinding) binding).getReturnType())) {
        J2ObjC.warning(
            "Warning: Ignoring AutoreleasePool annotation on method with non-void return type");
        return false;
      }
    }

    return hasAnnotation;
  }

  // JDT doesn't have any way to dynamically create a null literal binding.
  private static class NullType implements ITypeBinding {
    static final NullType SINGLETON = new NullType();

    public IAnnotationBinding[] getAnnotations() {
      return new IAnnotationBinding[0];
    }

    public int getKind() {
      return IBinding.TYPE;
    }

    public boolean isDeprecated() {
      return false;
    }

    public boolean isRecovered() {
      return false;
    }

    public boolean isSynthetic() {
      return false;
    }

    public IJavaElement getJavaElement() {
      return null;
    }

    public String getKey() {
      return null;
    }

    public boolean isEqualTo(IBinding binding) {
      return binding.getName().equals("null");
    }

    public ITypeBinding createArrayType(int dimension) {
      return null;
    }

    public String getBinaryName() {
      return "N";
    }

    public ITypeBinding getBound() {
      return null;
    }

    public ITypeBinding getGenericTypeOfWildcardType() {
      return null;
    }

    public int getRank() {
      return -1;
    }

    public ITypeBinding getComponentType() {
      return null;
    }

    public IVariableBinding[] getDeclaredFields() {
      return new IVariableBinding[0];
    }

    public IMethodBinding[] getDeclaredMethods() {
      return new IMethodBinding[0];
    }

    public int getDeclaredModifiers() {
      return 0;
    }

    public ITypeBinding[] getDeclaredTypes() {
      return new ITypeBinding[0];
    }

    public ITypeBinding getDeclaringClass() {
      return null;
    }

    public IMethodBinding getDeclaringMethod() {
      return null;
    }

    public int getDimensions() {
      return 0;
    }

    public ITypeBinding getElementType() {
      return null;
    }

    public ITypeBinding getErasure() {
      return null;
    }

    public ITypeBinding[] getInterfaces() {
      return new ITypeBinding[0];
    }

    public int getModifiers() {
      return 0;
    }

    public String getName() {
      return "null";
    }

    public IPackageBinding getPackage() {
      return null;
    }

    public String getQualifiedName() {
      return "null";
    }

    public ITypeBinding getSuperclass() {
      return null;
    }

    public ITypeBinding[] getTypeArguments() {
      return new ITypeBinding[0];
    }

    public ITypeBinding[] getTypeBounds() {
      return new ITypeBinding[0];
    }

    public ITypeBinding getTypeDeclaration() {
      return SINGLETON;
    }

    public ITypeBinding[] getTypeParameters() {
      return new ITypeBinding[0];
    }

    public ITypeBinding getWildcard() {
      return null;
    }

    public boolean isAnnotation() {
      return false;
    }

    public boolean isAnonymous() {
      return false;
    }

    public boolean isArray() {
      return false;
    }

    public boolean isAssignmentCompatible(ITypeBinding variableType) {
      return true;
    }

    public boolean isCapture() {
      return false;
    }

    public boolean isCastCompatible(ITypeBinding type) {
      return false;
    }

    public boolean isClass() {
      return false;
    }

    public boolean isEnum() {
      return false;
    }

    public boolean isFromSource() {
      return false;
    }

    public boolean isGenericType() {
      return false;
    }

    public boolean isInterface() {
      return false;
    }

    public boolean isLocal() {
      return false;
    }

    public boolean isMember() {
      return false;
    }

    public boolean isNested() {
      return false;
    }

    public boolean isNullType() {
      return true;
    }

    public boolean isParameterizedType() {
      return false;
    }

    public boolean isPrimitive() {
      return false;
    }

    public boolean isRawType() {
      return false;
    }

    public boolean isSubTypeCompatible(ITypeBinding type) {
      return false;
    }

    public boolean isTopLevel() {
      return false;
    }

    public boolean isTypeVariable() {
      return false;
    }

    public boolean isUpperbound() {
      return false;
    }

    public boolean isWildcardType() {
      return false;
    }
  }

  /**
   * Returns the signature of an element, defined in the Java Language
   * Specification 3rd edition, section 13.1.
   */
  @Override
  public String getSignature(IBinding binding) {
    if (binding instanceof ITypeBinding) {
      return ((ITypeBinding) binding).getBinaryName();
    }
    if (binding instanceof IMethodBinding) {
      return getSignature((IMethodBinding) binding);
    }
    return binding.getName();
  }

  private static String getSignature(IMethodBinding binding) {
    StringBuilder sb = new StringBuilder("(");
    for (ITypeBinding parameter : binding.getParameterTypes()) {
      appendParameterSignature(parameter.getErasure(), sb);
    }
    sb.append(')');
    if (binding.getReturnType() != null) {
      appendParameterSignature(binding.getReturnType().getErasure(), sb);
    }
    return sb.toString();
  }

  private static void appendParameterSignature(ITypeBinding parameter, StringBuilder sb) {
    if (!parameter.isPrimitive() && !parameter.isArray()) {
      sb.append('L');
    }
    sb.append(parameter.getBinaryName().replace('.', '/'));
    if (!parameter.isPrimitive() && !parameter.isArray()) {
      sb.append(';');
    }
  }
}
