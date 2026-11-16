import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SeverityBadge } from './SeverityBadge'

describe('SeverityBadge', () => {
  it('shows the score and band name with a band-specific class', () => {
    render(<SeverityBadge score={22} severity="CRITICAL" />)
    const badge = screen.getByTestId('severity-badge')
    expect(badge).toHaveTextContent('22 · Critical')
    expect(badge).toHaveClass('badge-critical')
  })
})
