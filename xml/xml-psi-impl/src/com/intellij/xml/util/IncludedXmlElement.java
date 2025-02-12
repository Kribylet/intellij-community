// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.xml.util;

import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;

public abstract class IncludedXmlElement<T extends XmlElement> extends LightElement implements XmlElement {
  private final PsiAnchor myOriginal;
  private SoftReference<T> myRef;
  private final PsiElement myParent;

  public IncludedXmlElement(@NotNull T original, @Nullable PsiElement parent) {
    super(original.getManager(), original.getLanguage());
    //noinspection unchecked
    T realOriginal = original instanceof IncludedXmlElement ? ((IncludedXmlElement<T>)original).getOriginal() : original;
    myOriginal = PsiAnchor.create(realOriginal);
    myRef = new SoftReference<>(realOriginal);
    myParent = parent;
  }

  @Override
  public boolean isValid() {
    T t = myRef.get();
    if (t != null) {
      return t.isValid();
    }

    return myOriginal.retrieve() != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IncludedXmlElement element = (IncludedXmlElement)o;

    if (!myParent.equals(element.myParent)) return false;
    if (!myOriginal.equals(element.myOriginal)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myOriginal.hashCode();
    result = 31 * result + myParent.hashCode();
    return result;
  }

  public T getOriginal() {
    T element = myRef.get();
    if (element != null) {
      return element;
    }

    element = (T)myOriginal.retrieve();
    if (element == null) {
      throw new PsiInvalidElementAccessException(this);
    }
    myRef = new SoftReference<>(element);
    return element;
  }

  @NotNull
  @Override
  public T getNavigationElement() {
    return getOriginal();
  }

  @Override
  public PsiFile getContainingFile() {
    return myParent.getContainingFile();
  }

  @Override
  public PsiElement getParent() {
    return myParent;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean processElements(final PsiElementProcessor processor, PsiElement place) {
    final IncludedXmlElement<T> self = this;
    return getOriginal().processElements(new PsiElementProcessor() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean execute(@NotNull PsiElement element) {
        if (element instanceof XmlTag) {
          XmlTag theirParent = ((XmlTag)element).getParentTag();
          PsiElement parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
          return processor.execute(new IncludedXmlTag((XmlTag)element, parent));
        }
        if (element instanceof XmlAttribute) {
          XmlTag theirParent = ((XmlAttribute)element).getParent();
          XmlTag parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
          return processor.execute(new IncludedXmlAttribute((XmlAttribute)element, parent));
        }
        if (element instanceof XmlText) {
          XmlTag theirParent = ((XmlText)element).getParentTag();
          XmlTag parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
          return processor.execute(new IncludedXmlText((XmlText)element, parent));
        }
        return processor.execute(element);
      }
    }, place);
  }


}
