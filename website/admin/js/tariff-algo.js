export function calculateBill(units, slabs, charges) {
  let energy = 0;
  let appliedSlabName = '';

  if (!slabs || slabs.length === 0) {
    return { energy: 0, fixedCharge: 0, tax: 0, otherFlat: 0, total: 0, mode: 'none' };
  }

  // Determine mode from first slab or charges
  const calcMode = slabs[0].calc_mode || 'incremental';

  if (calcMode === 'whole_rate') {
    // Find matching slab
    const matchingSlab = slabs.find(s => units >= s.from_unit && units <= s.to_unit) || slabs[slabs.length - 1];
    if (matchingSlab) {
      energy = units * (matchingSlab.rate_per_unit || 0);
      appliedSlabName = matchingSlab.tariff_name;
    }
  } else {
    // Incremental
    // Sort slabs by from_unit ascending
    const sortedSlabs = [...slabs].sort((a, b) => a.from_unit - b.from_unit);
    for (const slab of sortedSlabs) {
      if (units < slab.from_unit) continue;
      const upper = slab.to_unit || 999999;
      const unitsInSlab = Math.max(0, Math.min(units, upper) - slab.from_unit + 1);
      energy += unitsInSlab * (slab.rate_per_unit || 0);
    }
  }

  const fixedCharge = charges?.fixed_charge || 0;
  const gstPercent = charges?.gst_percent || 0;
  const otherPercent = charges?.other_percent || 0;
  const otherFlat = charges?.other_flat || 0;

  const subtotal = energy + fixedCharge;
  const tax = subtotal * ((gstPercent + otherPercent) / 100);
  const total = subtotal + tax + otherFlat;

  return {
    energy: Math.round(energy * 100) / 100,
    fixedCharge: Math.round(fixedCharge * 100) / 100,
    tax: Math.round(tax * 100) / 100,
    otherFlat: Math.round(otherFlat * 100) / 100,
    total: Math.round(total * 100) / 100,
    mode: calcMode
  };
}
