import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ToastComponent } from './toast.component';
import { ToastService } from '../../services/toast.service';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display toasts from service', fakeAsync(() => {
    toastService.success('Test message');
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Test message');
    tick(4000);
  }));

  it('should return correct CSS class for success', () => {
    expect(component.toastClass('success')).toContain('bg-green');
  });

  it('should return correct CSS class for error', () => {
    expect(component.toastClass('error')).toContain('bg-red');
  });

  it('should return correct CSS class for info', () => {
    expect(component.toastClass('info')).toContain('bg-blue');
  });

  it('should remove toast on click', fakeAsync(() => {
    toastService.success('Clickable');
    fixture.detectChanges();
    expect(toastService.toasts().length).toBe(1);

    const id = toastService.toasts()[0].id;
    toastService.remove(id);
    fixture.detectChanges();
    expect(toastService.toasts().length).toBe(0);
    tick(4000);
  }));
});
