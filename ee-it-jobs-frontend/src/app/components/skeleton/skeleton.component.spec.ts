import { TestBed, ComponentFixture } from '@angular/core/testing';
import { SkeletonComponent } from './skeleton.component';

describe('SkeletonComponent', () => {
  let component: SkeletonComponent;
  let fixture: ComponentFixture<SkeletonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default width 100%', () => {
    expect(component.width).toBe('100%');
  });

  it('should have default height 1rem', () => {
    expect(component.height).toBe('1rem');
  });

  it('should apply custom width and height', () => {
    component.width = '200px';
    component.height = '3rem';
    fixture.detectChanges();
    const div = fixture.nativeElement.querySelector('div');
    expect(div.style.width).toBe('200px');
    expect(div.style.height).toBe('3rem');
  });
});
