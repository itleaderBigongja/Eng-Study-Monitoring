// study-monitoring-frontend/src/app/api/refresh/route.ts
import { NextResponse } from 'next/server';

export async function POST() {
  return NextResponse.json({ message: 'Refresh endpoint' });
}
