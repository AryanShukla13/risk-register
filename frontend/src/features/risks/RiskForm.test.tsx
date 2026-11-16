import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { RiskForm } from './RiskForm'

describe('RiskForm', () => {
  it('updates the live inherent score when likelihood or impact change', async () => {
    render(<RiskForm submitLabel="Save" onSubmit={vi.fn()} />)
    const badge = () => screen.getByTestId('severity-badge')

    expect(badge()).toHaveTextContent('9 · Medium') // defaults 3 x 3

    await userEvent.selectOptions(screen.getByLabelText('Likelihood'), '5')
    await userEvent.selectOptions(screen.getByLabelText('Impact'), '4')
    expect(badge()).toHaveTextContent('20 · Critical')
  })

  it('blocks submit and shows errors when required fields are empty', async () => {
    const onSubmit = vi.fn()
    render(<RiskForm submitLabel="Save" onSubmit={onSubmit} />)
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
