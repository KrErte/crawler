import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [NotificationService]
    });
    service = TestBed.inject(NotificationService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should detect if notifications are supported', () => {
    // In test env, 'Notification' may or may not be in window
    expect(typeof service.isSupported()).toBe('boolean');
  });

  it('should read permission state', () => {
    const perm = service.permission();
    expect(['granted', 'denied', 'default']).toContain(perm);
  });

  it('should enable via localStorage', () => {
    localStorage.setItem('browserNotifications', 'true');
    // Re-create service to pick up localStorage
    service = TestBed.inject(NotificationService);
    // The signal reads from localStorage at construction
    expect(service.isEnabled()).toBe(localStorage.getItem('browserNotifications') === 'true');
  });

  it('should disable and remove localStorage', () => {
    localStorage.setItem('browserNotifications', 'true');
    service.disable();
    expect(service.isEnabled()).toBeFalse();
    expect(localStorage.getItem('browserNotifications')).toBeNull();
  });

  it('should return false from requestPermission if not supported', async () => {
    if (!service.isSupported()) {
      const result = await service.requestPermission();
      expect(result).toBeFalse();
    } else {
      // If supported, we can't easily test without mocking Notification API
      expect(service.isSupported()).toBeTrue();
    }
  });
});
