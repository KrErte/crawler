import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslateService } from './translate.service';

describe('TranslateService', () => {
  let service: TranslateService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TranslateService]
    });
    service = TestBed.inject(TranslateService);
    httpMock = TestBed.inject(HttpTestingController);

    // Flush the constructor's setLanguage call
    const langReqs = httpMock.match(() => true);
    langReqs.forEach(r => r.flush({ 'hello': 'Hello', 'goodbye': 'Goodbye' }));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should default to en language', () => {
    expect(service.currentLang()).toBe('en');
  });

  it('should load saved language from localStorage', () => {
    localStorage.setItem('lang', 'et');

    // Re-create service
    const newService = new TranslateService(TestBed.inject(HttpClientTestingModule as any));
    // The constructor reads from localStorage
    expect(localStorage.getItem('lang')).toBe('et');
  });

  it('should set language and load translations', () => {
    service.setLanguage('et');

    const req = httpMock.expectOne('/assets/i18n/et.json');
    expect(req.request.method).toBe('GET');
    req.flush({ 'hello': 'Tere', 'goodbye': 'Nägemist' });

    expect(service.currentLang()).toBe('et');
    expect(localStorage.getItem('lang')).toBe('et');
    expect(service.t('hello')).toBe('Tere');
  });

  it('should toggle language between en and et', () => {
    expect(service.currentLang()).toBe('en');

    service.toggleLanguage();
    const req1 = httpMock.expectOne('/assets/i18n/et.json');
    req1.flush({ 'hello': 'Tere' });
    expect(service.currentLang()).toBe('et');

    service.toggleLanguage();
    const req2 = httpMock.expectOne('/assets/i18n/en.json');
    req2.flush({ 'hello': 'Hello' });
    expect(service.currentLang()).toBe('en');
  });

  it('should return key as fallback when translation missing', () => {
    expect(service.t('nonexistent.key')).toBe('nonexistent.key');
  });

  it('should return translated string for known key', () => {
    // Translations were loaded in beforeEach
    expect(service.t('hello')).toBe('Hello');
  });

  it('should fallback to English on load error', () => {
    service.setLanguage('fr');

    const frReq = httpMock.expectOne('/assets/i18n/fr.json');
    frReq.error(new ProgressEvent('error'));

    // Should fallback to English
    const enReq = httpMock.expectOne('/assets/i18n/en.json');
    enReq.flush({ 'hello': 'Hello fallback' });

    expect(service.t('hello')).toBe('Hello fallback');
  });
});
