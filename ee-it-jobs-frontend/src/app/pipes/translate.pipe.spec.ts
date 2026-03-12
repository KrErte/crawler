import { TranslatePipe } from './translate.pipe';
import { TranslateService } from '../services/translate.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let translateSpy: jasmine.SpyObj<TranslateService>;

  beforeEach(() => {
    translateSpy = jasmine.createSpyObj('TranslateService', ['t']);
    pipe = new TranslatePipe(translateSpy as any);
  });

  it('should create', () => {
    expect(pipe).toBeTruthy();
  });

  it('should call translateService.t() with key', () => {
    translateSpy.t.and.returnValue('Hello');
    pipe.transform('hello');
    expect(translateSpy.t).toHaveBeenCalledWith('hello');
  });

  it('should return translated string', () => {
    translateSpy.t.and.returnValue('Tere');
    const result = pipe.transform('hello');
    expect(result).toBe('Tere');
  });

  it('should return key when no translation found', () => {
    translateSpy.t.and.returnValue('unknown.key');
    const result = pipe.transform('unknown.key');
    expect(result).toBe('unknown.key');
  });
});
