import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ToastService]
    });
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with empty toasts', () => {
    expect(service.toasts().length).toBe(0);
  });

  it('should add success toast', () => {
    service.success('Operation completed');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].type).toBe('success');
    expect(service.toasts()[0].message).toBe('Operation completed');
  });

  it('should add error toast', () => {
    service.error('Something went wrong');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].type).toBe('error');
  });

  it('should add info toast', () => {
    service.info('FYI');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].type).toBe('info');
  });

  it('should auto-remove toast after 4 seconds', fakeAsync(() => {
    service.success('Temp message');
    expect(service.toasts().length).toBe(1);

    tick(3999);
    expect(service.toasts().length).toBe(1);

    tick(1);
    expect(service.toasts().length).toBe(0);
  }));

  it('should remove toast by id', fakeAsync(() => {
    service.success('First');
    service.error('Second');
    expect(service.toasts().length).toBe(2);

    const firstId = service.toasts()[0].id;
    service.remove(firstId);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].message).toBe('Second');

    // Clean up remaining timer
    tick(4000);
  }));

  it('should assign unique ids to toasts', () => {
    service.success('A');
    service.error('B');
    service.info('C');

    const ids = service.toasts().map(t => t.id);
    const uniqueIds = new Set(ids);
    expect(uniqueIds.size).toBe(3);
  });
});
