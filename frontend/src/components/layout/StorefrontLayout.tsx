import { useState } from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { ShoppingCart, ShoppingBag } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useCartStore } from '@/store/cartStore'
import { Button, buttonVariants } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'
import CartDrawer from '@/components/cart/CartDrawer'

export default function StorefrontLayout() {
  const { token, user, logout } = useAuthStore()
  const itemCount = useCartStore((s) => s.items.reduce((sum, i) => sum + i.quantity, 0))
  const [cartOpen, setCartOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="border-b border-primary/15 sticky top-0 z-40 bg-background/90 backdrop-blur">
        <nav className="mx-auto max-w-7xl px-6 h-14 flex items-center justify-between">
          {/* Left */}
          <Link to="/" className="flex items-center gap-2 group">
            <div className="p-1.5 rounded-md bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <ShoppingBag className="h-4 w-4 text-primary" />
            </div>
            <span className="font-black text-sm tracking-widest">AGENTIC<span className="text-primary">.</span></span>
          </Link>

          {/* Centre */}
          <div className="flex items-center gap-6">
            <Link to="/" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
              Products
            </Link>
            {token && (
              <Link to="/orders" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                Orders
              </Link>
            )}
          </div>

          {/* Right */}
          <div className="flex items-center gap-2">
            {!token ? (
              <>
                <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>
                  Login
                </Button>
                <Button size="sm" onClick={() => navigate('/register')}>
                  Register
                </Button>
              </>
            ) : (
              <DropdownMenu>
                <DropdownMenuTrigger className={cn(buttonVariants({ variant: 'ghost', size: 'sm' }), 'max-w-[180px] truncate')}>
                  {user?.email}
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {user?.role === 'ADMIN' && (
                    <DropdownMenuItem onClick={() => navigate('/admin')}>
                      Admin Panel
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuItem
                    onClick={() => { logout(); navigate('/') }}
                    className="text-destructive focus:text-destructive"
                  >
                    Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
            <Button
              variant="ghost"
              size="icon"
              className="relative"
              onClick={() => setCartOpen(true)}
              aria-label={`Cart, ${itemCount} items`}
            >
              <ShoppingCart className="h-5 w-5" />
              {itemCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 bg-primary text-primary-foreground text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {itemCount > 9 ? '9+' : itemCount}
                </span>
              )}
            </Button>
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8" style={{ animation: 'page-in 0.2s ease-out' }}>
        <Outlet />
      </main>

      <CartDrawer open={cartOpen} onClose={() => setCartOpen(false)} />
    </div>
  )
}
