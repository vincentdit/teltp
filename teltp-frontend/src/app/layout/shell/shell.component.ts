import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { Router } from '@angular/router';

interface NavItem { label: string; link: string; roles?: string[]; requiresAuth?: boolean; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly tokens = inject(TokenService);
  private readonly router = inject(Router);

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly username = this.auth.username;

  private readonly allNav: NavItem[] = [
    { label: 'Catalogue', link: '/catalog' },
    { label: 'My Learning', link: '/dashboard', requiresAuth: true },
    { label: 'My Certificates', link: '/certificates', requiresAuth: true },
    { label: 'Administration', link: '/admin', roles: ['ADMIN', 'INSTRUCTOR'] },
  ];

  readonly nav = computed<NavItem[]>(() =>
    this.allNav.filter((item) => {
      if (item.roles) return this.tokens.hasAnyRole(item.roles as never);
      if (item.requiresAuth) return this.isAuthenticated();
      return true;
    }),
  );

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
