import { describe, expect, it } from 'vitest'
import { inherentScore, severityBand } from './scoring'

describe('severityBand', () => {
  it.each([
    [1, 'LOW'], [5, 'LOW'],
    [6, 'MEDIUM'], [12, 'MEDIUM'],
    [13, 'HIGH'], [19, 'HIGH'],
    [20, 'CRITICAL'], [25, 'CRITICAL'],
  ])('score %i is %s', (score, expected) => {
    expect(severityBand(score)).toBe(expected)
  })
})

describe('inherentScore', () => {
  it('multiplies likelihood by impact', () => {
    expect(inherentScore(4, 5)).toBe(20)
    expect(inherentScore(1, 1)).toBe(1)
  })
})
