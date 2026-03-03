import { Directive, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { I18nService } from '../services/i18n.service';

@Directive({
  selector: '[appAutoTranslate]',
  standalone: true
})
export class AutoTranslateDirective implements OnInit, OnDestroy {
  private readonly originalTextNodes = new WeakMap<Text, string>();
  private readonly lastTranslatedTextNodes = new WeakMap<Text, string>();
  private readonly originalAttributes = new WeakMap<Element, Map<string, string>>();
  private readonly lastTranslatedAttributes = new WeakMap<Element, Map<string, string>>();
  private observer?: MutationObserver;
  private languageSubscription?: Subscription;

  constructor(
    private readonly host: ElementRef<HTMLElement>,
    private readonly i18nService: I18nService
  ) {}

  ngOnInit(): void {
    this.applyTranslations();

    this.languageSubscription = this.i18nService.languageChanges$.subscribe(() => {
      this.applyTranslations();
    });

    this.observer = new MutationObserver(() => {
      this.applyTranslations();
    });
    this.observer.observe(this.host.nativeElement, {
      childList: true,
      subtree: true,
      characterData: true,
      attributes: true,
      attributeFilter: ['placeholder', 'title', 'aria-label', 'mattooltip']
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.languageSubscription?.unsubscribe();
  }

  private applyTranslations(): void {
    this.translateTextNodes();
    this.translateAttributes();
  }

  private translateTextNodes(): void {
    const root = this.host.nativeElement;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let current = walker.nextNode();

    while (current) {
      const textNode = current as Text;
      const parentElement = textNode.parentElement;
      if (!parentElement || this.shouldSkipElement(parentElement)) {
        current = walker.nextNode();
        continue;
      }

      const currentText = textNode.textContent ?? '';
      const previousOriginal = this.originalTextNodes.get(textNode);
      const previousTranslated = this.lastTranslatedTextNodes.get(textNode);

      let original = previousOriginal ?? currentText;
      if (!this.originalTextNodes.has(textNode)) {
        this.originalTextNodes.set(textNode, original);
      } else {
        const wasUpdatedByAngular = currentText !== (previousTranslated ?? currentText) && currentText !== previousOriginal;
        const wasInitiallyEmpty = (!previousOriginal || !previousOriginal.trim()) && !!currentText.trim();
        if (wasUpdatedByAngular || wasInitiallyEmpty) {
          original = currentText;
          this.originalTextNodes.set(textNode, original);
        }
      }

      const translated = this.i18nService.translateLiteral(original);
      if (textNode.textContent !== translated) {
        textNode.textContent = translated;
      }
      this.lastTranslatedTextNodes.set(textNode, translated);

      current = walker.nextNode();
    }
  }

  private translateAttributes(): void {
    const attributesToTranslate = ['placeholder', 'title', 'aria-label', 'mattooltip'];
    const allElements = this.host.nativeElement.querySelectorAll('*');

    allElements.forEach((element) => {
      if (this.shouldSkipElement(element)) {
        return;
      }

      attributesToTranslate.forEach((attributeName) => {
        if (!element.hasAttribute(attributeName)) {
          return;
        }

        const originalAttributeMap = this.originalAttributes.get(element) ?? new Map<string, string>();
        if (!this.originalAttributes.has(element)) {
          this.originalAttributes.set(element, originalAttributeMap);
        }
        const translatedAttributeMap = this.lastTranslatedAttributes.get(element) ?? new Map<string, string>();
        if (!this.lastTranslatedAttributes.has(element)) {
          this.lastTranslatedAttributes.set(element, translatedAttributeMap);
        }

        const currentValue = element.getAttribute(attributeName) ?? '';
        const previousOriginalValue = originalAttributeMap.get(attributeName);
        const previousTranslatedValue = translatedAttributeMap.get(attributeName);

        let originalValue = previousOriginalValue ?? currentValue;
        if (!originalAttributeMap.has(attributeName)) {
          originalAttributeMap.set(attributeName, originalValue);
        } else {
          const wasUpdatedByAngular = currentValue !== (previousTranslatedValue ?? currentValue) && currentValue !== previousOriginalValue;
          const wasInitiallyEmpty = (!previousOriginalValue || !previousOriginalValue.trim()) && !!currentValue.trim();
          if (wasUpdatedByAngular || wasInitiallyEmpty) {
            originalValue = currentValue;
            originalAttributeMap.set(attributeName, originalValue);
          }
        }

        const translated = this.i18nService.translateLiteral(originalValue);
        if (element.getAttribute(attributeName) !== translated) {
          element.setAttribute(attributeName, translated);
        }
        translatedAttributeMap.set(attributeName, translated);
      });
    });
  }

  private shouldSkipElement(element: Element): boolean {
    const tagName = element.tagName.toLowerCase();
    if (tagName === 'script' || tagName === 'style' || tagName === 'noscript' || tagName === 'mat-icon') {
      return true;
    }

    return !!element.closest('mat-icon');
  }
}
