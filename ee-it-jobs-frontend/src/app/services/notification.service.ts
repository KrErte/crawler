import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly isSupported = signal('Notification' in window);
  readonly permission = signal<NotificationPermission>(
    'Notification' in window ? Notification.permission : 'denied'
  );
  readonly isEnabled = signal(
    typeof localStorage !== 'undefined' && localStorage.getItem('browserNotifications') === 'true'
  );

  async requestPermission(): Promise<boolean> {
    if (!this.isSupported()) return false;

    const result = await Notification.requestPermission();
    this.permission.set(result);

    if (result === 'granted') {
      this.isEnabled.set(true);
      localStorage.setItem('browserNotifications', 'true');
      return true;
    }

    return false;
  }

  disable(): void {
    this.isEnabled.set(false);
    localStorage.removeItem('browserNotifications');
  }

  showNotification(title: string, options?: NotificationOptions): void {
    if (!this.isSupported() || !this.isEnabled() || this.permission() !== 'granted') return;

    const notification = new Notification(title, {
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      ...options
    });

    notification.onclick = () => {
      window.focus();
      notification.close();
    };

    // Auto-close after 8 seconds
    setTimeout(() => notification.close(), 8000);
  }
}
