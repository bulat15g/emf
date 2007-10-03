/**
 * <copyright>
 * </copyright>
 *
 * $Id: Supplier.java,v 1.1 2004/08/20 22:47:32 marcelop Exp $
 */
package org.eclipse.emf.test.core.featuremap.supplier;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Supplier</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.test.core.featuremap.supplier.Supplier#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.emf.test.core.featuremap.supplier.Supplier#getOrders <em>Orders</em>}</li>
 *   <li>{@link org.eclipse.emf.test.core.featuremap.supplier.Supplier#getPreferredOrders <em>Preferred Orders</em>}</li>
 *   <li>{@link org.eclipse.emf.test.core.featuremap.supplier.Supplier#getStandardOrders <em>Standard Orders</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.test.core.featuremap.supplier.SupplierPackage#getSupplier()
 * @model 
 * @generated
 */
public interface Supplier extends EObject
{
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Name</em>' attribute.
   * @see #setName(String)
   * @see org.eclipse.emf.test.core.featuremap.supplier.SupplierPackage#getSupplier_Name()
   * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.eclipse.emf.test.core.featuremap.supplier.Supplier#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Orders</b></em>' attribute list.
   * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Orders</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Orders</em>' attribute list.
   * @see org.eclipse.emf.test.core.featuremap.supplier.SupplierPackage#getSupplier_Orders()
   * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
   * @generated
   */
  FeatureMap getOrders();

  /**
   * Returns the value of the '<em><b>Preferred Orders</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.test.core.featuremap.supplier.PurchaseOrder}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Preferred Orders</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Preferred Orders</em>' containment reference list.
   * @see org.eclipse.emf.test.core.featuremap.supplier.SupplierPackage#getSupplier_PreferredOrders()
   * @model type="org.eclipse.emf.test.core.featuremap.supplier.PurchaseOrder" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
   * @generated
   */
  EList getPreferredOrders();

  /**
   * Returns the value of the '<em><b>Standard Orders</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.test.core.featuremap.supplier.PurchaseOrder}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Standard Orders</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Standard Orders</em>' containment reference list.
   * @see org.eclipse.emf.test.core.featuremap.supplier.SupplierPackage#getSupplier_StandardOrders()
   * @model type="org.eclipse.emf.test.core.featuremap.supplier.PurchaseOrder" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
   * @generated
   */
  EList getStandardOrders();

} // Supplier