package com.google.devtools.j2objc.types;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public interface TypeService {

	// The first argument of a iOS method isn't named, but Java requires some sort of valid parameter
	// name.  The method mapper therefore uses this string, which the generators ignore.
	public static final String EMPTY_PARAMETER_NAME = "__empty_parameter__";
	public static final String NS_ANY_TYPE = "NS_ANY_TYPE"; // type of "id"

	void initialize(CompilationUnit unit);

	/**
	 * If this method overrides another method, return the binding for the
	 * original declaration.
	 */
	IMethodBinding getOriginalMethodBinding(IMethodBinding method);

	/**
	 * Returns true if the specified binding is for a static final variable.
	 */
	boolean isConstantVariable(IVariableBinding binding);

	boolean isStaticVariable(IVariableBinding binding);

	boolean isPrimitiveConstant(IVariableBinding binding);

	/**
	 * Given a JDT type binding created by the parser, either replace it with an iOS
	 * equivalent, or return the given type.
	 */
	ITypeBinding mapType(ITypeBinding binding);

	/**
	 * Given a fully-qualified type name, return its binding.
	 */
	ITypeBinding mapTypeName(String typeName);

	/**
	 * Returns whether a given type has an iOS equivalent.
	 */
	boolean hasIOSEquivalent(ITypeBinding binding);

	/**
	 * Returns true if a Type AST node refers to an iOS type.
	 */
	boolean isIOSType(Type type);

	/**
	 * Returns true if a type name refers to an iOS type.
	 */
	boolean isIOSType(String name);

	/**
	 * Returns a simple (no package) name for a given one.
	 */
	String mapSimpleTypeName(String typeName);

	/**
	 * Returns a Type AST node for a specific type binding.
	 */
	Type makeType(ITypeBinding binding);

	/**
	 * Creates a replacement iOS type for a given JDT type.
	 */
	Type makeIOSType(Type type);

	Type makeIOSType(ITypeBinding binding);

	/**
	 * Returns true if a specified method binding refers to a replacement iOS
	 * type.
	 */
	boolean isMappedMethod(IMethodBinding method);

	void addMappedIOSMethod(IMethodBinding binding, IOSMethod method);

	IOSMethod getMappedMethod(IMethodBinding binding);

	/**
	 * Returns true if a specified variable binding refers has a replacement.
	 */
	boolean isMappedVariable(IVariableBinding var);

	void addMappedVariable(ASTNode node, IVariableBinding newBinding);

	IVariableBinding getMappedVariable(IVariableBinding binding);

	void addMappedInvocation(Expression method, IMethodBinding binding);

	IMethodBinding resolveInvocationBinding(Expression invocation);

	IOSTypeBinding resolveIOSType(String name);

	boolean isJavaObjectType(ITypeBinding type);

	boolean isJavaStringType(ITypeBinding type);

	boolean isJavaNumberType(ITypeBinding type);

	boolean isFloatingPointType(ITypeBinding type);

	boolean isBooleanType(ITypeBinding type);

	ITypeBinding resolveIOSType(Type type);

	IOSTypeBinding resolveArrayType(String name);

	IOSArrayTypeBinding resolveArrayType(ITypeBinding binding);

	String getPrimitiveTypeName(ITypeBinding binding);

	IBinding getBinding(Object node);

	void addBinding(Object node, IBinding binding);

	/**
	 * Return a type binding for a specified ASTNode or IOS node, or null if
	 * no type binding exists.
	 */
	ITypeBinding getTypeBinding(Object node);

	IMethodBinding getMethodBinding(Object node);

	IVariableBinding getVariableBinding(Object node);

	/**
	 * Walks an AST and asserts there is a resolved binding for every
	 * ASTNode type that is supposed to have one.
	 */
	void verifyNode(ASTNode node);

	void verifyNodes(List<? extends ASTNode> nodes);

	void substitute(ASTNode oldNode, ASTNode replacement);

	ASTNode getNode(ASTNode currentNode);

	ITypeBinding renameTypeBinding(String newName,
			ITypeBinding newDeclaringClass, ITypeBinding originalBinding);

	ITypeBinding getRenamedBinding(ITypeBinding original);

	void addFunction(IMethodBinding binding);

	boolean isFunction(IMethodBinding binding);

	boolean isVoidType(Type type);

	boolean isVoidType(ITypeBinding type);

	boolean isJavaVoidType(ITypeBinding type);

	/**
	 * Returns the declaration for a specified binding from a list of
	 * type declarations.
	 */
	TypeDeclaration getTypeDeclaration(ITypeBinding binding,
			List<?> declarations);

	/**
	 * Adds a variable that needs to be cast when referenced.  This is necessary
	 * for gcc to verify parameters of generic interface's methods
	 */
	void addVariableCast(IVariableBinding var, ITypeBinding castType);

	boolean variableHasCast(IVariableBinding var);

	ITypeBinding getCastForVariable(IVariableBinding var);

	void addReleaseableFields(Collection<IVariableBinding> fields);

	boolean isReleaseableField(IVariableBinding var);

	NullLiteral newNullLiteral();

	SimpleName newLabel(String identifier);

	boolean isJUnitTest(ITypeBinding type);

	ITypeBinding getWrapperType(ITypeBinding primitiveType);

	ITypeBinding getPrimitiveType(ITypeBinding wrapperType);

	ITypeBinding getNSNumber();

	ITypeBinding getNSObject();

	ITypeBinding getNSString();

	ITypeBinding getIOSClass();

	boolean isWeakReference(IVariableBinding var);

	boolean hasAnyAnnotation(IBinding binding, Class<?>[] annotations);

	boolean hasAnnotation(IBinding binding, Class<?> annotation);

	boolean hasWeakAnnotation(IBinding binding);

	boolean hasAutoreleasePoolAnnotation(IBinding binding);

	/**
	 * Returns the signature of an element, defined in the Java Language
	 * Specification 3rd edition, section 13.1.
	 */
	String getSignature(IBinding binding);

}