import { Outlet, Link, useLocation } from 'react-router-dom'
import { Package, Receipt, ArrowLeft } from 'lucide-react'
import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  { label: 'Products', href: '/admin/products', icon: Package, section: 'Catalog' },
  { label: 'Orders', href: '/admin/orders', icon: Receipt, section: 'Commerce' },
]

export default function AdminLayout() {
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen bg-background flex">
      {/* Sidebar */}
      <aside className="w-56 bg-sidebar flex flex-col border-r border-sidebar-border flex-shrink-0">
        <div className="px-4 py-4">
          <span className="text-xs font-bold tracking-widest text-primary">ADMIN</span>
        </div>

        <nav className="flex-1 px-3 space-y-4">
          {['Catalog', 'Commerce'].map((section) => (
            <div key={section}>
              <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1 px-2">
                {section}
              </p>
              {NAV_ITEMS.filter((i) => i.section === section).map(({ label, href, icon: Icon }) => (
                <Link
                  key={href}
                  to={href}
                  className={cn(
                    'flex items-center gap-2 px-2 py-2 rounded-md text-sm transition-colors',
                    pathname.startsWith(href)
                      ? 'bg-accent text-foreground'
                      : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'
                  )}
                >
                  <Icon className="h-4 w-4 flex-shrink-0" />
                  {label}
                </Link>
              ))}
            </div>
          ))}
        </nav>

        <div className="p-3 border-t border-sidebar-border">
          <Link
            to="/"
            className="flex items-center gap-2 px-2 py-2 text-sm text-muted-foreground hover:text-foreground transition-colors rounded-md hover:bg-accent/50"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Storefront
          </Link>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 p-8 text-foreground overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
