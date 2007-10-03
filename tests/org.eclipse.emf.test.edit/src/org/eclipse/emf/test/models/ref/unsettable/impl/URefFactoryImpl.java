/**
 * <copyright>
 * </copyright>
 *
 * $Id: URefFactoryImpl.java,v 1.1 2005/08/09 04:43:08 davidms Exp $
 */
package org.eclipse.emf.test.models.ref.unsettable.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.test.models.ref.unsettable.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class URefFactoryImpl extends EFactoryImpl implements URefFactory
{
  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public URefFactoryImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass)
  {
    switch (eClass.getClassifierID())
    {
      case URefPackage.C1U: return createC1U();
      case URefPackage.C2U: return createC2U();
      case URefPackage.AU: return createAU();
      case URefPackage.BU: return createBU();
      case URefPackage.CU: return createCU();
      case URefPackage.DU: return createDU();
      case URefPackage.C4U: return createC4U();
      case URefPackage.C3U: return createC3U();
      case URefPackage.EU: return createEU();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public C1U createC1U()
  {
    C1UImpl c1U = new C1UImpl();
    return c1U;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public C2U createC2U()
  {
    C2UImpl c2U = new C2UImpl();
    return c2U;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public AU createAU()
  {
    AUImpl au = new AUImpl();
    return au;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BU createBU()
  {
    BUImpl bu = new BUImpl();
    return bu;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public CU createCU()
  {
    CUImpl cu = new CUImpl();
    return cu;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public DU createDU()
  {
    DUImpl du = new DUImpl();
    return du;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public C4U createC4U()
  {
    C4UImpl c4U = new C4UImpl();
    return c4U;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public C3U createC3U()
  {
    C3UImpl c3U = new C3UImpl();
    return c3U;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EU createEU()
  {
    EUImpl eu = new EUImpl();
    return eu;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public URefPackage getURefPackage()
  {
    return (URefPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static URefPackage getPackage()
  {
    return URefPackage.eINSTANCE;
  }

} //URefFactoryImpl