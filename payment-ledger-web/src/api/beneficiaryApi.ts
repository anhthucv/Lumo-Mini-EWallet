import type {
  Beneficiary,
  CreateBeneficiaryRequest,
  UpdateBeneficiaryRequest,
} from '../types/beneficiary';
import { requestJson } from './http';

export function getBeneficiaries(signal?: AbortSignal): Promise<Beneficiary[]> {
  return requestJson<Beneficiary[]>('/beneficiaries', {
    method: 'GET',
    signal,
  });
}

export function createBeneficiary(
  request: CreateBeneficiaryRequest,
  signal?: AbortSignal,
): Promise<Beneficiary> {
  return requestJson<Beneficiary>('/beneficiaries', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}

export function updateBeneficiary(
  id: number,
  request: UpdateBeneficiaryRequest,
  signal?: AbortSignal,
): Promise<Beneficiary> {
  return requestJson<Beneficiary>(`/beneficiaries/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
    signal,
  });
}

export function deleteBeneficiary(id: number, signal?: AbortSignal): Promise<void> {
  return requestJson<void>(`/beneficiaries/${id}`, {
    method: 'DELETE',
    signal,
  });
}
